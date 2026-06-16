# app.py
# Python AI Visualization Microservice for the MyFlow Project
# ENHANCED with Protective Factor Analysis and Best Day Tracking

import io
import base64
import matplotlib
matplotlib.use('Agg')
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from flask import Flask, request, jsonify
import math
from statistics import mean
from collections import defaultdict


from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error


app = Flask(__name__)

def calculate_daily_metrics(data):
    df = pd.json_normalize(data)
    df['cognitive_load.study_minutes'] = pd.to_numeric(
        df['cognitive_load.study_minutes'], errors='coerce').fillna(0)
    df['normalized_study'] = (df['cognitive_load.study_minutes'].clip(upper=900) / 900) * 10
    df['positive_custom_impact'] = 0
    df['negative_custom_impact'] = 0

    def process_custom(row):
        pos_impact = 0
        neg_impact = 0
        if isinstance(row, list):
            for factor in row:
                effect = factor.get('effect', 0)
                impact = effect
                if impact > 0:
                    pos_impact += impact
                elif impact < 0:
                    neg_impact += impact
        return pos_impact, neg_impact

    df[['positive_custom_impact', 'negative_custom_impact']] = df['custom'].apply(
        lambda x: pd.Series(process_custom(x)))
    df['emotional.stress'] = pd.to_numeric(df['emotional.stress'], errors='coerce').fillna(0)
    df['total_negative_load'] = (
        df['emotional.stress'] +
        df['normalized_study'] +
        df['positive_custom_impact']
    )

    # Ensure sleep column exists even if missing
    if 'physiological.sleep_hours' not in df.columns:
        df['physiological.sleep_hours'] = 0
        
    metrics_df = pd.DataFrame({
        'date': pd.to_datetime(df['date']),
        'tics': pd.to_numeric(df['symptoms.tic_count'], errors='coerce').fillna(0),
        'TNL': df['total_negative_load'],
        'stress_contrib': df['emotional.stress'],
        'study_contrib': df['normalized_study'],
        'pos_custom_contrib': df['positive_custom_impact'],
        'neg_custom_contrib': df['negative_custom_impact'].abs(),
        'raw_neg_impact': df['negative_custom_impact'],
        'sleep': pd.to_numeric(df['physiological.sleep_hours'], errors='coerce').fillna(0)
    })
    return metrics_df.set_index('date').sort_index()

def analyze_protective_factors(data):
    factor_data = defaultdict(lambda: {
        'days_used': [],
        'tic_counts': [],
        'total_impact': 0,
        'usage_count': 0
    })

    daily_info = []
    all_tic_counts = []

    for entry in data:
        date = entry.get('date', 'Unknown')
        tic_count = entry.get('symptoms', {}).get('tic_count', 0)
        stress = entry.get('emotional', {}).get('stress', 0)
        study = entry.get('cognitive_load', {}).get('study_minutes', 0)
        custom_factors = entry.get('custom', [])

        all_tic_counts.append(tic_count)

        normalized_study = min(study, 900) / 900 * 10
        pos_impact = 0
        neg_impact = 0
        protective_factors_today = []

        for factor in custom_factors:
            name = factor.get('name', 'Unknown')
            effect = factor.get('effect', 0)
            impact = effect

            if impact < 0:
                neg_impact += impact
                protective_factors_today.append({'name': name, 'impact': abs(impact)})
                factor_data[name]['days_used'].append(date)
                factor_data[name]['tic_counts'].append(tic_count)
                factor_data[name]['total_impact'] += abs(impact)
                factor_data[name]['usage_count'] += 1
            elif impact > 0:
                pos_impact += impact

        tnl = stress + normalized_study + pos_impact
        daily_info.append({
            'date': date,
            'tnl': tnl,
            'tics': tic_count,
            'stress': stress,
            'study_minutes': study,
            'protective_factors': protective_factors_today,
            'total_protection': abs(neg_impact)
        })

    # -------------------------------------------------------
    # FIX: Use overall average as baseline for ALL factors.
    # Previously used "days without this factor" which was 
    # polluted by other high/low factor days, causing negative
    # reductions. Overall average is a fair, stable baseline.
    # -------------------------------------------------------
    overall_avg_tics = mean(all_tic_counts) if all_tic_counts else 0

    factor_analysis = []

    for factor_name, factor_info in factor_data.items():
        if factor_info['usage_count'] >= 1:
            avg_impact = factor_info['total_impact'] / factor_info['usage_count']
            days_with_factor_tics = factor_info['tic_counts']
            avg_tics_with = mean(days_with_factor_tics) if days_with_factor_tics else 0

            # Use overall average as the "without" baseline
            avg_tics_without = overall_avg_tics

            if avg_tics_without > 0:
                tic_reduction_pct = ((avg_tics_without - avg_tics_with) / avg_tics_without) * 100
            else:
                tic_reduction_pct = 0

            factor_analysis.append({
                'name': factor_name,
                'avg_impact': round(avg_impact, 2),
                'times_used': factor_info['usage_count'],
                'avg_tics_with': round(avg_tics_with, 1),
                'avg_tics_without': round(avg_tics_without, 1),
                'tic_reduction_pct': round(tic_reduction_pct, 1)
            })

    factor_analysis.sort(key=lambda x: x['tic_reduction_pct'], reverse=True)
    best_factor = factor_analysis[0] if factor_analysis else None

    if daily_info:
        lowest_tnl_day = min(daily_info, key=lambda x: x['tnl'])
        top_3_best_days = sorted(daily_info, key=lambda x: x['tnl'])[:3]
    else:
        lowest_tnl_day = None
        top_3_best_days = []

    return {
        'best_protective_factor': best_factor,
        'all_protective_factors': factor_analysis[:5],
        'lowest_tnl_day': lowest_tnl_day,
        'top_3_best_days': top_3_best_days,
        'total_days_analyzed': len(daily_info)
    }


def generate_protective_factor_insight(analysis):
    insights = []
    best = analysis.get('best_protective_factor')
    if best:
        name = best['name']
        reduction = best['tic_reduction_pct']
        times_used = best['times_used']
        avg_with = best['avg_tics_with']
        avg_without = best['avg_tics_without']

        rare_factors = ['vacation', 'vacation day', 'holiday', 'beach trip', 'travel', 'recovery day', 'sick day', 'day off']
        is_rare = any(rare in name.lower() for rare in rare_factors)

        if reduction > 20:
            if is_rare:
                all_factors = analysis.get('all_protective_factors', [])
                regular_alternatives = [
                    f for f in all_factors
                    if not any(rare in f['name'].lower() for rare in rare_factors)
                    and f['tic_reduction_pct'] > 0
                ]
                if regular_alternatives:
                    alt = regular_alternatives[0]
                    insights.append(
                        f"🏆 **Your MVP Protective Factor: {name}!** "
                        f"When you use this, your tics drop by {reduction:.0f}% on average "
                        f"(from {avg_without:.1f} to {avg_with:.1f} tics). "
                        f"But since you can't take a {name.lower()} every day, try **{alt['name']}** instead - "
                        f"it's your best daily option with a {alt['tic_reduction_pct']:.0f}% reduction!"
                    )
                else:
                    insights.append(
                        f"🏆 **Your MVP Protective Factor: {name}!** "
                        f"When you use this, your tics drop by {reduction:.0f}% on average. "
                        f"Since you can't do this daily, try to capture what makes it helpful and add small versions to your routine."
                    )
            else:
                insights.append(
                    f"🏆 **Your MVP Protective Factor: {name}!** "
                    f"When you use this, your tics drop by {reduction:.0f}% on average "
                    f"(from {avg_without:.1f} to {avg_with:.1f} tics). "
                    f"You've used it {times_used} time(s) - this is your secret weapon for high-stress days!"
                )
        elif reduction > 10:
            insights.append(
                f"⭐ **Top Helper: {name}** reduces your tics by about {reduction:.0f}%. "
                f"(Average {avg_with:.1f} tics with it vs {avg_without:.1f} without). Keep using it!"
            )
        elif reduction > 0:
            insights.append(
                f"💡 **{name}** shows promise with a {reduction:.0f}% tic reduction. "
                f"Try using it more consistently to see stronger effects."
            )
        else:
            insights.append(f"📊 **{name}** is being tracked but needs more data to see patterns.")

    lowest = analysis.get('lowest_tnl_day')
    if lowest:
        factors_used = [f['name'] for f in lowest['protective_factors']]
        if factors_used:
            factors_str = ", ".join(factors_used)
            insights.append(
                f"🌟 **Your Best Day: {lowest['date']}** - TNL was only {lowest['tnl']:.1f} with just {lowest['tics']:.0f} tics! "
                f"What worked: {factors_str}. Try to recreate these conditions!"
            )
        else:
            insights.append(f"🌟 **Your Best Day: {lowest['date']}** - TNL was {lowest['tnl']:.1f} with {lowest['tics']:.0f} tics.")

    all_factors = analysis.get('all_protective_factors', [])
    if len(all_factors) >= 2:
        ranking_lines = []
        for i, f in enumerate(all_factors[:5], 1):
            emoji = "🥇" if i == 1 else "🥈" if i == 2 else "🥉" if i == 3 else f"{i}."
            ranking_lines.append(f"{emoji} {f['name']}: {f['tic_reduction_pct']:.0f}% reduction (used {f['times_used']}x)")
        insights.append("📋 **Your Protective Factor Ranking:**\n" + "\n".join(ranking_lines))

    return "\n\n".join(insights) if insights else "Track more days with protective factors to see insights!"


def generate_pacing_recommendation(df):
    if len(df) < 7:
        return {
            "pacing_state": "Baseline Needed",
            "message": "Continue tracking data for 7 days to establish your personalized baseline for Adaptive Pacing."
        }

    latest_day = df.iloc[-1]
    baseline_df = df.iloc[-8:-1] if len(df) >= 8 else df.iloc[:-1]

    mu_load = baseline_df['TNL'].mean()
    sigma_load = baseline_df['TNL'].std()
    mu_tics = baseline_df['tics'].mean()
    sigma_tics = baseline_df['tics'].std()

    threshold_load = mu_load + sigma_load
    threshold_tics = mu_tics + sigma_tics

    load_spiking = latest_day['TNL'] > threshold_load
    tics_spiking = latest_day['tics'] > threshold_tics

    if load_spiking and tics_spiking:
        state = "ADAPTIVE PACING ALERT"
        msg = (
            "⚠️ **ADAPTIVE PACING ALERT** for {}. Both your Total Negative Load ({:.1f}) and Tic Frequency ({:.0f}) are spiking. "
            "Recommendation: **Switch to Micro-Goals** immediately. Break down all tasks into 15-minute blocks and prioritize recovery.".format(
                latest_day.name.strftime("%b %d"), latest_day['TNL'], latest_day['tics'])
        )
    elif load_spiking and not tics_spiking:
        state = "HIGH LOAD WARNING"
        msg = (
            "⚠️ **HIGH LOAD WARNING** for {}. Your Total Negative Load ({:.1f}) is spiking, but symptoms are stable. "
            "Recommendation: **Preventative Rest.** Schedule a 30-minute break before attempting the next task.".format(
                latest_day.name.strftime("%b %d"), latest_day['TNL'])
        )
    elif not load_spiking and tics_spiking:
        state = "UNUSUAL SPIKE"
        msg = (
            "💡 **UNUSUAL SPIKE** for {}. Tic Frequency ({:.0f}) is spiking, but your calculated Load is normal. "
            "Recommendation: **Re-Evaluate Custom Factors.** A new, untracked trigger may be at play. Track it now!".format(
                latest_day.name.strftime("%b %d"), latest_day['tics'])
        )
    else:
        state = "GREEN LIGHT"
        msg = (
            "✅ **GREEN LIGHT!** Your load and symptoms are stable and within the normal range. "
            "Recommendation: **Maintain Momentum.** Your current pacing and coping strategies are effective. Continue with your scheduled goals."
        )

    return {
        "pacing_state": state,
        "message": msg,
        "latest_load": float(latest_day['TNL']),
        "load_threshold": float(threshold_load)
    }


def generate_visualization(df):
    plt.style.use('ggplot')
    fig, ax = plt.subplots(figsize=(10, 5))
    dates = df.index.strftime('%Y-%m-%d').tolist()
    bottom = np.zeros(len(df))
    stress_bars = ax.bar(dates, df['stress_contrib'], label='Stress (0-10)', color='#FF8C42', bottom=bottom)
    bottom += df['stress_contrib']
    study_bars = ax.bar(dates, df['study_contrib'], label='Cognitive Load (Normalized)', color='#43AA8B', bottom=bottom)
    bottom += df['study_contrib']
    pos_custom_bars = ax.bar(dates, df['pos_custom_contrib'], label='Positive Custom Impact', color='#2D7DD2', bottom=bottom)
    neg_custom_bars = ax.bar(dates, df['raw_neg_impact'], label='Protective Custom Factor', color='#F5E663')
    ax2 = ax.twinx()
    tic_line = ax2.plot(dates, df['tics'], color='#EE4266', marker='o', label='Tic Count (Trend)')
    ax.set_title('MyFlow Daily Factor Contributions vs. Tic Count', fontsize=14)
    ax.set_xlabel('Date')
    ax.set_ylabel('Factor Contribution Score / Load Reduction', color='black')
    ax.tick_params(axis='y', labelcolor='black')
    ax.axhline(0, color='grey', linewidth=0.8)
    ax2.set_ylabel('Tic Count (Target Symptom)', color='#EE4266')
    ax2.tick_params(axis='y', labelcolor='#EE4266')
    bars = [stress_bars[0], study_bars[0], pos_custom_bars[0], neg_custom_bars[0]]
    labels = [b.get_label() for b in bars]
    labels.append(tic_line[0].get_label())
    ax.legend(bars + tic_line, labels, loc='upper left', fontsize=8)
    ax.set_xticks(range(len(dates)))
    ax.set_xticklabels([d[5:] for d in dates], rotation=45, ha='right', fontsize=8)
    plt.tight_layout()
    img_stream = io.BytesIO()
    plt.savefig(img_stream, format='png')
    plt.close(fig)
    img_stream.seek(0)
    img_base64 = base64.b64encode(img_stream.read()).decode('utf-8')
    return img_base64


def calculate_sleep_analysis(data):
    if len(data) < 7:
        return {'error': 'Not enough data. Track for at least 7 days.', 'avg_sleep': None, 'correlation': None}
    recent_data = data[-14:] if len(data) >= 14 else data
    sleep_hours = []
    tic_counts = []
    for entry in recent_data:
        sleep = entry.get('physiological', {}).get('sleep_hours', 0)
        tic_count = entry.get('symptoms', {}).get('tic_count', 0)
        if sleep > 0:
            sleep_hours.append(float(sleep))
            tic_counts.append(float(tic_count))
    if len(sleep_hours) < 5:
        return {'error': 'Not enough days with sleep data logged.', 'avg_sleep': None, 'correlation': None}
    avg_sleep = round(mean(sleep_hours), 1)
    correlation = calculate_pearson_correlation(sleep_hours, tic_counts)
    paired = sorted(zip(tic_counts, sleep_hours), key=lambda x: x[0])
    best_days_sleep = [s for _, s in paired[:len(paired)//3]]
    worst_days_sleep = [s for _, s in paired[-(len(paired)//3):]]
    optimal_sleep = round(mean(best_days_sleep), 1) if best_days_sleep else avg_sleep
    worst_sleep = round(mean(worst_days_sleep), 1) if worst_days_sleep else avg_sleep
    tolerance = 0.75
    good_sleep_tics = [t for s, t in zip(sleep_hours, tic_counts) if abs(s - optimal_sleep) <= tolerance]
    bad_sleep_tics = [t for s, t in zip(sleep_hours, tic_counts) if abs(s - optimal_sleep) > tolerance * 2]
    avg_good_sleep_tics = round(mean(good_sleep_tics), 1) if good_sleep_tics else None
    avg_bad_sleep_tics = round(mean(bad_sleep_tics), 1) if bad_sleep_tics else None
    if avg_good_sleep_tics and avg_bad_sleep_tics and avg_good_sleep_tics > 0:
        percent_diff = round(((avg_bad_sleep_tics - avg_good_sleep_tics) / avg_good_sleep_tics) * 100)
    else:
        percent_diff = None
    insight = generate_sleep_insight(avg_sleep, correlation, percent_diff, avg_good_sleep_tics, avg_bad_sleep_tics, optimal_sleep=optimal_sleep, worst_sleep=worst_sleep)
    return {
        'avg_sleep_hours': avg_sleep, 'correlation_coefficient': correlation,
        'optimal_sleep_hours': optimal_sleep, 'avg_tics_good_sleep': avg_good_sleep_tics,
        'avg_tics_bad_sleep': avg_bad_sleep_tics, 'percent_difference': percent_diff,
        'insight_message': insight, 'days_analyzed': len(sleep_hours), 'success': True
    }


def calculate_pearson_correlation(x_values, y_values):
    if len(x_values) < 2 or len(y_values) < 2:
        return 0.0
    n = len(x_values)
    mean_x = mean(x_values)
    mean_y = mean(y_values)
    covariance = sum((x_values[i] - mean_x) * (y_values[i] - mean_y) for i in range(n))
    std_x = math.sqrt(sum((x - mean_x) ** 2 for x in x_values))
    std_y = math.sqrt(sum((y - mean_y) ** 2 for y in y_values))
    if std_x == 0 or std_y == 0:
        return 0.0
    return round(covariance / (std_x * std_y), 2)


def generate_sleep_insight(avg_sleep, correlation, percent_diff, good_tics, bad_tics, optimal_sleep=7.5, worst_sleep=None):
    if avg_sleep < 6:
        sleep_status = "⚠️ You're averaging only {:.1f} hours of sleep per night - that's below the recommended 7-9 hours for teens.".format(avg_sleep)
    elif avg_sleep < 7:
        sleep_status = "You're averaging {:.1f} hours of sleep per night - close to the recommended 7-9 hour range.".format(avg_sleep)
    elif avg_sleep <= 9:
        sleep_status = "✅ You're averaging {:.1f} hours of sleep per night - that's in the healthy range!".format(avg_sleep)
    else:
        sleep_status = "You're averaging {:.1f} hours of sleep per night.".format(avg_sleep)
    if correlation <= -0.7:
        corr_text = " Your sleep-tic connection is VERY STRONG ({:.2f}). More sleep dramatically reduces your tic count!".format(correlation)
    elif correlation <= -0.5:
        corr_text = " Your sleep-tic connection is STRONG ({:.2f}). More sleep significantly reduces your tics.".format(correlation)
    elif correlation <= -0.3:
        corr_text = " Your sleep-tic connection is MODERATE ({:.2f}). More sleep tends to reduce your tics.".format(correlation)
    elif correlation <= -0.1:
        corr_text = " Your sleep-tic connection is WEAK ({:.2f}). Sleep has a small effect on your tics.".format(correlation)
    elif correlation < 0.1:
        corr_text = " Your sleep-tic connection is VERY WEAK ({:.2f}). Other factors might be more important.".format(correlation)
    else:
        corr_text = " Your sleep-tic connection is POSITIVE ({:.2f}). Sleep quality or other factors may matter more than quantity.".format(correlation)
    if good_tics is not None and bad_tics is not None and percent_diff is not None:
        if percent_diff > 0:
            comparison = f" Your personal sweet spot is around {optimal_sleep}hrs — tics average {good_tics:.1f} on those nights vs {bad_tics:.1f} on others — a {percent_diff}% difference!"
        else:
            comparison = f" Nights near {optimal_sleep}hrs give you {good_tics:.1f} avg tics vs {bad_tics:.1f} on other nights."
    else:
        comparison = " Track a few more days to compare tic levels across sleep amounts."
    return sleep_status + corr_text + comparison


@app.route('/api/protective-factors', methods=['POST'])
def get_protective_factor_analysis():
    if not request.json:
        return jsonify({"error": "Missing JSON payload"}), 400
    data = request.json
    try:
        analysis = analyze_protective_factors(data)
        insight_text = generate_protective_factor_insight(analysis)
        best = analysis.get('best_protective_factor')
        lowest = analysis.get('lowest_tnl_day')
        return jsonify({
            "success": True,
            "insight_message": insight_text,
            "best_factor_name": best['name'] if best else None,
            "best_factor_reduction": best['tic_reduction_pct'] if best else None,
            "best_factor_times_used": best['times_used'] if best else None,
            "best_factor_avg_tics_with": best['avg_tics_with'] if best else None,
            "best_factor_avg_tics_without": best['avg_tics_without'] if best else None,
            "all_factors": analysis.get('all_protective_factors', []),
            "lowest_tnl_date": lowest['date'] if lowest else None,
            "lowest_tnl_value": lowest['tnl'] if lowest else None,
            "lowest_tnl_tics": lowest['tics'] if lowest else None,
            "lowest_tnl_factors": [f['name'] for f in lowest['protective_factors']] if lowest else [],
            "top_3_best_days": [
                {'date': d['date'], 'tnl': d['tnl'], 'tics': d['tics'], 'factors': [f['name'] for f in d['protective_factors']]}
                for d in analysis.get('top_3_best_days', [])
            ],
            "days_analyzed": analysis.get('total_days_analyzed', 0)
        })
    except Exception as e:
        print(f"Error in protective factor analysis: {e}")
        return jsonify({"error": f"Protective factor analysis failed: {str(e)}", "success": False}), 500
    


@app.route('/api/regression', methods=['POST'])
def get_regression():
    if not request.json:
        return jsonify({"error": "Missing JSON payload"}), 400
    data = request.json
    try:
        metrics_df = calculate_daily_metrics(data)
        
        if len(metrics_df) < 3:
            return jsonify({"error": "Need at least 3 days of data for regression", "success": False}), 400
        
        tnl_values = metrics_df['TNL'].values
        tic_values = metrics_df['tics'].values
        
        # fit a degree-1 polynomial (straight line) — returns [slope, intercept]
        slope, intercept = np.polyfit(tnl_values, tic_values, 1)
        
        # calculate R² manually
        predicted = slope * tnl_values + intercept
        ss_res = np.sum((tic_values - predicted) ** 2)
        ss_tot = np.sum((tic_values - np.mean(tic_values)) ** 2)
        r_squared = 1 - (ss_res / ss_tot) if ss_tot != 0 else 0
        
        # build a human-readable interpretation
        direction = "increases" if slope > 0 else "decreases"
        interpretation = (
            f"For every 1-unit increase in Total Negative Load, "
            f"your tic count {direction} by {abs(slope):.2f}. "
            f"This model explains {r_squared * 100:.1f}% of your tic variation (R²={r_squared:.2f})."
        )
        
        return jsonify({
            "success": True,
            "slope": round(slope, 3),
            "intercept": round(intercept, 3),
            "r_squared": round(r_squared, 3),
            "interpretation": interpretation,
            "days_analyzed": len(metrics_df)
        })
    
    except Exception as e:
        print(f"Regression error: {e}")
        return jsonify({"error": f"Regression failed: {str(e)}", "success": False}), 500


@app.route('/api/sleep-analysis', methods=['POST'])
def get_sleep_analysis():
    if not request.json:
        return jsonify({"error": "Missing JSON payload"}), 400
    data = request.json
    try:
        analysis = calculate_sleep_analysis(data)
        return jsonify(analysis)
    except Exception as e:
        return jsonify({"error": f"Sleep analysis failed: {str(e)}", "success": False}), 500


@app.route('/api/visualization', methods=['POST'])
def get_visualization():
    if not request.json:
        return jsonify({"error": "Missing JSON payload"}), 400
    data = request.json
    try:

        metrics_df = calculate_daily_metrics(data)

        recommendation = generate_pacing_recommendation(metrics_df)
        img_base64 = generate_visualization(metrics_df)

       
       # ---------------------------------------------------------
        # 🚀 THE ML ENGINE: CHAMPION VS CHALLENGER
        # ---------------------------------------------------------
        forecast_summary = "Not enough data for forecasting."
        forecast_equation = "Need at least 5 days of data."
        forecast_evaluation = "Keep tracking to unlock ML predictions!"
        
        if len(metrics_df) >= 5:
            # 1. Feature Engineering (The Lagged Dataset)
            ml_df = metrics_df.copy()
            ml_df['tics_tomorrow'] = ml_df['tics'].shift(-1)
            ml_df['sleep_squared'] = ml_df['sleep'] ** 2
            
            train_df = ml_df.dropna(subset=['tics_tomorrow'])
            y_train = train_df['tics_tomorrow']
            
            # 2. Train Model A: Standard Linear (Straight Line)
            X_linear = train_df[['TNL', 'sleep']]
            model_linear = LinearRegression()
            model_linear.fit(X_linear, y_train)
            mae_linear = mean_absolute_error(y_train, model_linear.predict(X_linear))
            
            # 3. Train Model B: Polynomial (U-Shaped Curve)
            X_poly = train_df[['TNL', 'sleep', 'sleep_squared']]
            model_poly = LinearRegression()
            model_poly.fit(X_poly, y_train)
            mae_poly = mean_absolute_error(y_train, model_poly.predict(X_poly))
            
            # 4. Determine the Champion (Which model has lower error?)
            latest_day = metrics_df.iloc[-1]
            
            if mae_poly < mae_linear:
                # Polynomial Won
                X_latest = pd.DataFrame({'TNL': [latest_day['TNL']], 'sleep': [latest_day['sleep']], 'sleep_squared': [latest_day['sleep'] ** 2]})
                tomorrow_pred = max(0, model_poly.predict(X_latest)[0])
                
                finding = f"Polynomial models outperformed linear models (MAE {mae_poly:.2f} vs {mae_linear:.2f}), suggesting non-linear biological relationships."
                forecast_equation = f"Tics = ({model_poly.coef_[0]:.2f}×TNL) + ({model_poly.coef_[1]:.2f}×Sleep) + ({model_poly.coef_[2]:.2f}×Sleep²) + {model_poly.intercept_:.2f}"
                
            else:
                # Linear Won
                X_latest = pd.DataFrame({'TNL': [latest_day['TNL']], 'sleep': [latest_day['sleep']]})
                tomorrow_pred = max(0, model_linear.predict(X_latest)[0])
                
                finding = f"Linear models outperformed polynomial models (MAE {mae_linear:.2f} vs {mae_poly:.2f}), suggesting symptoms scale directly with load."
                forecast_equation = f"Tics = ({model_linear.coef_[0]:.2f}×TNL) + ({model_linear.coef_[1]:.2f}×Sleep) + {model_linear.intercept_:.2f}"
            
            # 5. Format Outputs for Java
            forecast_summary = f"Predicted Tics: {tomorrow_pred:.1f}"
            forecast_evaluation = f"**Scientific Finding:** {finding}"

        # Original single-variable regression logic
        tnl_values = metrics_df['TNL'].values
        tic_values = metrics_df['tics'].values
        slope, intercept_single = np.polyfit(tnl_values, tic_values, 1)
        predicted_single = slope * tnl_values + intercept_single
        ss_res = np.sum((tic_values - predicted_single) ** 2)
        ss_tot = np.sum((tic_values - np.mean(tic_values)) ** 2)
        r_squared = 1 - (ss_res / ss_tot) if ss_tot != 0 else 0
        direction = "increases" if slope > 0 else "decreases"
        regression_text = (
            f"For every 1-unit increase in Total Negative Load, "
            f"your tic count {direction} by {abs(slope):.2f}. "
            f"This model explains {r_squared * 100:.1f}% of your tic variation (R²={r_squared:.2f})."
        )

        return jsonify({
            "pacing_recommendation": recommendation["message"],
            "pacing_state": recommendation["pacing_state"],
            "latest_load": recommendation.get("latest_load"),
            "load_threshold": recommendation.get("load_threshold"),
            "graph_image_base64": img_base64,
            "regression_insight": regression_text,
            "r_squared": round(r_squared, 3),
            "forecast_summary": forecast_summary,
            "forecast_equation": forecast_equation,
            "forecast_evaluation": forecast_evaluation,
            "success": True
        })
    except Exception as e:
        print(f"An error occurred during analysis: {e}")
        return jsonify({"error": f"Internal Server Error during analysis: {str(e)}"}), 500
    

if __name__ == '__main__':
    print("\n" + "="*50)
    print("🚀 Starting MyFlow Python AI Microservice (Flask)")
    print("="*50)
    print("API Endpoints:")
    print("  POST /api/visualization")
    print("  POST /api/sleep-analysis")
    print("  POST /api/protective-factors")
    print("  POST /api/regression  <-- NEW!")
    
    print("Server running on http:/AQZ!Œ/127.0.0.1:5000")
    print("="*50 + "\n")
    app.run(debug=True, port=5000, host='127.0.0.1')