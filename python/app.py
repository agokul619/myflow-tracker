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

app = Flask(__name__)

# --- HELPER FUNCTIONS ---

def calculate_daily_metrics(data):
    """
    Processes the raw JSON data to calculate normalized metrics,
    the composite TNL score, and separates custom impacts.
    """
    df = pd.json_normalize(data)

    df['cognitive_load.study_minutes'] = pd.to_numeric(
        df['cognitive_load.study_minutes'], errors='coerce').fillna(0)
    
    df['normalized_study'] = (
        df['cognitive_load.study_minutes'].clip(upper=900) / 900) * 10
    
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
    
    metrics_df = pd.DataFrame({
        'date': pd.to_datetime(df['date']),
        'tics': pd.to_numeric(df['symptoms.tic_count'], errors='coerce').fillna(0),
        'TNL': df['total_negative_load'],
        'stress_contrib': df['emotional.stress'],
        'study_contrib': df['normalized_study'],
        'pos_custom_contrib': df['positive_custom_impact'],
        'neg_custom_contrib': df['negative_custom_impact'].abs(), 
        'raw_neg_impact': df['negative_custom_impact']
    })

    return metrics_df.set_index('date').sort_index()


# ============================================================
# NEW FUNCTION: Analyze Protective Factors
# ============================================================
def analyze_protective_factors(data):
    """
    Analyzes which protective (negative effect) custom factors
    are most effective at reducing tics.
    
    Returns:
    - Best protective factor name and its average impact
    - Correlation between each protective factor and tic counts
    - Days with lowest TNL and what factors were present
    """
    
    # Track each protective factor's usage and corresponding tic counts
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
        
        # Calculate TNL for this day
        normalized_study = min(study, 900) / 900 * 10
        pos_impact = 0
        neg_impact = 0
        protective_factors_today = []
        
        for factor in custom_factors:
            name = factor.get('name', 'Unknown')
            level = factor.get('level', 0)
            effect = factor.get('effect', 0)
            impact = level * effect
            
            if impact < 0:  # Protective factor!
                neg_impact += impact
                protective_factors_today.append({
                    'name': name,
                    'impact': abs(impact),
                    'level': level
                })
                # Track this factor's data
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
    
    # Analyze each protective factor
    factor_analysis = []
    
    for factor_name, factor_info in factor_data.items():
        if factor_info['usage_count'] >= 1:
            avg_impact = factor_info['total_impact'] / factor_info['usage_count']
            
            # Calculate average tics WITH this factor vs WITHOUT
            days_with_factor_tics = factor_info['tic_counts']
            days_without_factor_tics = [
                d['tics'] for d in daily_info 
                if d['date'] not in factor_info['days_used']
            ]
            
            avg_tics_with = mean(days_with_factor_tics) if days_with_factor_tics else 0
            avg_tics_without = mean(days_without_factor_tics) if days_without_factor_tics else avg_tics_with
            
            # Calculate tic reduction percentage
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
    
    # Sort by effectiveness (tic reduction)
    factor_analysis.sort(key=lambda x: x['tic_reduction_pct'], reverse=True)
    
    # Find best protective factor
    best_factor = factor_analysis[0] if factor_analysis else None
    
    # Find the day with lowest TNL
    if daily_info:
        lowest_tnl_day = min(daily_info, key=lambda x: x['tnl'])
        top_3_best_days = sorted(daily_info, key=lambda x: x['tnl'])[:3]
    else:
        lowest_tnl_day = None
        top_3_best_days = []
    
    return {
        'best_protective_factor': best_factor,
        'all_protective_factors': factor_analysis[:5],  # Top 5
        'lowest_tnl_day': lowest_tnl_day,
        'top_3_best_days': top_3_best_days,
        'total_days_analyzed': len(daily_info)
    }


# ============================================================
# NEW FUNCTION: Generate Protective Factor Insight Text
# ============================================================

def generate_protective_factor_insight(analysis):
    """Generates human-friendly insights about protective factors."""
    insights = []
    
    # Best factor insight
    best = analysis.get('best_protective_factor')
    if best:
        name = best['name']
        reduction = best['tic_reduction_pct']
        times_used = best['times_used']
        avg_with = best['avg_tics_with']
        avg_without = best['avg_tics_without']
        
        # Check if this is a "rare" factor that can't be done daily
        rare_factors = ['vacation', 'vacation day', 'holiday', 'beach trip', 'travel', 'recovery day', 'sick day', 'day off']
        is_rare = any(rare in name.lower() for rare in rare_factors)
        
        if reduction > 20:
            if is_rare:
                # Find the best REGULAR factor to recommend instead
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
                        f"Since you can't do this daily, try to capture what makes it helpful "
                        f"(rest? nature? no stress?) and add small versions to your routine."
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
            insights.append(
                f"📊 **{name}** is being tracked but needs more data to see patterns."
            )
    
    # Lowest TNL day insight
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
            insights.append(
                f"🌟 **Your Best Day: {lowest['date']}** - TNL was {lowest['tnl']:.1f} with {lowest['tics']:.0f} tics."
            )
    
    # Ranking of all protective factors
    all_factors = analysis.get('all_protective_factors', [])
    if len(all_factors) >= 2:
        ranking_lines = []
        for i, f in enumerate(all_factors[:5], 1):
            emoji = "🥇" if i == 1 else "🥈" if i == 2 else "🥉" if i == 3 else f"{i}."
            ranking_lines.append(
                f"{emoji} {f['name']}: {f['tic_reduction_pct']:.0f}% reduction (used {f['times_used']}x)"
            )
        insights.append("📋 **Your Protective Factor Ranking:**\n" + "\n".join(ranking_lines))
    
    # THIS WAS MISSING!
    return "\n\n".join(insights) if insights else "Track more days with protective factors to see insights!"





def generate_pacing_recommendation(df):
    """
    Uses the Mean + 1 Standard Deviation (1-sigma rule) to determine if the 
    latest day is a 'spike' and generates an adaptive pacing recommendation.
    """
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
                latest_day.name.strftime("%b %d"), latest_day['TNL'], latest_day['tics']
            )
        )
    elif load_spiking and not tics_spiking:
        state = "HIGH LOAD WARNING"
        msg = (
            "⚠️ **HIGH LOAD WARNING** for {}. Your Total Negative Load ({:.1f}) is spiking, but symptoms are stable. "
            "Recommendation: **Preventative Rest.** You are coping well, but burnout is imminent. Schedule a 30-minute break before attempting the next task.".format(
                latest_day.name.strftime("%b %d"), latest_day['TNL']
            )
        )
    elif not load_spiking and tics_spiking:
        state = "UNUSUAL SPIKE"
        msg = (
            "💡 **UNUSUAL SPIKE** for {}. Tic Frequency ({:.0f}) is spiking, but your calculated Load is normal. "
            "Recommendation: **Re-Evaluate Custom Factors.** A new, untracked trigger (like poor diet or sudden weather change) may be at play. Track it now!".format(
                latest_day.name.strftime("%b %d"), latest_day['tics']
            )
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
    """
    Generates the stacked bar chart using Matplotlib and returns the Base64 image.
    """
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
        return {
            'error': 'Not enough data. Track for at least 7 days.',
            'avg_sleep': None,
            'correlation': None
        }
    
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
        return {
            'error': 'Not enough days with sleep data logged.',
            'avg_sleep': None,
            'correlation': None
        }
    
    avg_sleep = round(mean(sleep_hours), 1)
    correlation = calculate_pearson_correlation(sleep_hours, tic_counts)
    
    # --- SMART: find this person's optimal sleep range from their data ---
    # Sort days by tic count and find what sleep hours the best days share
    paired = sorted(zip(tic_counts, sleep_hours), key=lambda x: x[0])
    
    best_days_sleep = [s for _, s in paired[:len(paired)//3]]   # bottom third tics = best days
    worst_days_sleep = [s for _, s in paired[-(len(paired)//3):]]  # top third tics = worst days
    
    optimal_sleep = round(mean(best_days_sleep), 1) if best_days_sleep else avg_sleep
    worst_sleep = round(mean(worst_days_sleep), 1) if worst_days_sleep else avg_sleep
    
    # Build "good" and "bad" buckets using a window around optimal
    tolerance = 0.75
    good_sleep_tics = [t for s, t in zip(sleep_hours, tic_counts)
                       if abs(s - optimal_sleep) <= tolerance]
    bad_sleep_tics  = [t for s, t in zip(sleep_hours, tic_counts)
                       if abs(s - optimal_sleep) > tolerance * 2]
    
    avg_good_sleep_tics = round(mean(good_sleep_tics), 1) if good_sleep_tics else None
    avg_bad_sleep_tics  = round(mean(bad_sleep_tics), 1)  if bad_sleep_tics  else None
    
    if avg_good_sleep_tics and avg_bad_sleep_tics and avg_good_sleep_tics > 0:
        percent_diff = round(
            ((avg_bad_sleep_tics - avg_good_sleep_tics) / avg_good_sleep_tics) * 100
        )
    else:
        percent_diff = None
    
    insight = generate_sleep_insight(
        avg_sleep, correlation, percent_diff,
        avg_good_sleep_tics, avg_bad_sleep_tics,
        optimal_sleep=optimal_sleep,
        worst_sleep=worst_sleep
    )
    
    return {
        'avg_sleep_hours': avg_sleep,
        'correlation_coefficient': correlation,
        'optimal_sleep_hours': optimal_sleep,
        'avg_tics_good_sleep': avg_good_sleep_tics,
        'avg_tics_bad_sleep': avg_bad_sleep_tics,
        'percent_difference': percent_diff,
        'insight_message': insight,
        'days_analyzed': len(sleep_hours),
        'success': True
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
    
    correlation = covariance / (std_x * std_y)
    return round(correlation, 2)


def generate_sleep_insight(avg_sleep, correlation, percent_diff, 
                            good_tics, bad_tics, optimal_sleep=7.5, worst_sleep=None):
    
    if avg_sleep < 6:
        sleep_status = "⚠️ You're averaging only {:.1f} hours of sleep per night - that's below the recommended 7-9 hours for teens.".format(avg_sleep)
    elif avg_sleep < 7:
        sleep_status = "You're averaging {:.1f} hours of sleep per night - close to the recommended 7-9 hour range.".format(avg_sleep)
    elif avg_sleep <= 9:
        sleep_status = "✅ You're averaging {:.1f} hours of sleep per night - that's in the healthy range!".format(avg_sleep)
    else:
        sleep_status = "You're averaging {:.1f} hours of sleep per night - more than the typical 7-9 hour recommendation.".format(avg_sleep)
    
    if correlation <= -0.7:
        corr_text = " Your sleep-tic connection is VERY STRONG ({:.2f}). The closer this number is to -1, the more sleep helps reduce your tics. Your data shows more sleep dramatically reduces your tic count!".format(correlation)
    elif correlation <= -0.5:
        corr_text = " Your sleep-tic connection is STRONG ({:.2f}). The closer to -1, the stronger the connection. More sleep significantly reduces your tics.".format(correlation)
    elif correlation <= -0.3:
        corr_text = " Your sleep-tic connection is MODERATE ({:.2f}). Numbers closer to -1 mean stronger connection. More sleep tends to reduce your tics.".format(correlation)
    elif correlation <= -0.1:
        corr_text = " Your sleep-tic connection is WEAK ({:.2f}). This means sleep has a small effect on your tics. Other factors might be more important.".format(correlation)
    elif correlation < 0.1:
        corr_text = " Your sleep-tic connection is VERY WEAK ({:.2f}). Sleep doesn't show a clear pattern with your tics yet. Other factors are likely more important.".format(correlation)
    else:
        corr_text = " Your sleep-tic connection is POSITIVE ({:.2f}). This is unusual - it suggests more sleep correlates with more tics. This likely means other factors (sleep quality, stress, diet) are more important than sleep quantity.".format(correlation)
    
    if good_tics is not None and bad_tics is not None and percent_diff is not None:
        if percent_diff > 0:
            comparison = (
                f" Your data shows your personal sweet spot is around "
                f"{optimal_sleep}hrs — on those nights your tics average {good_tics:.1f}. "
                f"When sleep deviates significantly, tics rise to {bad_tics:.1f} "
                f"— that's a {percent_diff}% difference!"
            )
        elif percent_diff < 0:
            comparison = (
                f" Interestingly, nights near {optimal_sleep}hrs give you "
                f"{good_tics:.1f} avg tics vs {bad_tics:.1f} on other nights."
            )
        else:
            comparison = " Your tic levels are similar across different sleep amounts."
    else:
        comparison = " Track a few more days to compare tic levels across sleep amounts."
    
    return sleep_status + corr_text + comparison


# ============================================================
# NEW API ENDPOINT: Protective Factor Analysis
# ============================================================
@app.route('/api/protective-factors', methods=['POST'])
def get_protective_factor_analysis():
    """
    NEW endpoint that analyzes protective factors and returns insights.
    """
    if not request.json:
        return jsonify({"error": "Missing JSON payload"}), 400
    
    data = request.json
    
    try:
        analysis = analyze_protective_factors(data)
        insight_text = generate_protective_factor_insight(analysis)
        
        # Format for API response
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
                {
                    'date': d['date'],
                    'tnl': d['tnl'],
                    'tics': d['tics'],
                    'factors': [f['name'] for f in d['protective_factors']]
                }
                for d in analysis.get('top_3_best_days', [])
            ],
            "days_analyzed": analysis.get('total_days_analyzed', 0)
        })
        
    except Exception as e:
        print(f"Error in protective factor analysis: {e}")
        return jsonify({
            "error": f"Protective factor analysis failed: {str(e)}",
            "success": False
        }), 500


@app.route('/api/sleep-analysis', methods=['POST'])
def get_sleep_analysis():
    if not request.json:
        return jsonify({"error": "Missing JSON payload"}), 400
    
    data = request.json
    
    try:
        analysis = calculate_sleep_analysis(data)
        return jsonify(analysis)
    except Exception as e:
        print(f"Error in sleep analysis: {e}")
        return jsonify({
            "error": f"Sleep analysis failed: {str(e)}",
            "success": False
        }), 500


@app.route('/api/visualization', methods=['POST'])
def get_visualization():
    """
    Receives JSON data, runs analysis, and returns the result.
    """
    if not request.json:
        return jsonify({"error": "Missing JSON payload"}), 400

    data = request.json
    
    try:
        metrics_df = calculate_daily_metrics(data)
        recommendation = generate_pacing_recommendation(metrics_df)
        img_base64 = generate_visualization(metrics_df)

        return jsonify({
            "pacing_recommendation": recommendation["message"],
            "pacing_state": recommendation["pacing_state"],
            "latest_load": recommendation.get("latest_load"),
            "load_threshold": recommendation.get("load_threshold"),
            "graph_image_base64": img_base64,
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
    print("  POST /api/protective-factors  <-- NEW!")
    print("Server running on http://127.0.0.1:5000")
    print("="*50 + "\n")
    app.run(debug=True, port=5000, host='127.0.0.1')