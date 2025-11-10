# analysis_logic.py
# Contains all the core logic: data processing, statistical modeling, 
# TNL calculation, sleep vulnerability check, and Matplotlib visualization.

import io
import base64
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

# --- CONSTANTS FOR SENSITIVITY TUNING ---
# Used for normalizing Cognitive Load (Study Minutes) to a 0-10 scale
STUDY_MINUTES_MAX = 900 

# Used for the Sleep Deficit Penalty (8 hours max is considered healthy)
SLEEP_HOURS_THRESHOLD = 8.0 
SLEEP_PENALTY_WEIGHT = 1.5 
# --- END CONSTANTS ---


def determine_sleep_vulnerability(df):
    """
    Performs a meta-analysis on the historical data to determine if low sleep
    is a confirmed vulnerability factor for this specific user.
    """
    # Only use historical data (excluding the very last day being analyzed)
    baseline_df = df.iloc[:-1] if len(df) > 1 else df
    
    # 1. Define 'Low Sleep' and 'High Symptoms' conditions
    LOW_SLEEP = 6.0  # Threshold below which sleep is considered risky
    HIGH_TICS = 5    # Threshold above which tic level is considered symptomatic

    # 2. Filter data for periods of Low Sleep
    low_sleep_days = baseline_df[baseline_df['physiological.sleep_hours'] <= LOW_SLEEP]

    if low_sleep_days.empty or len(low_sleep_days) < 3:
        # Not enough data points to establish a pattern
        return False, "Insufficient data points (<3) with low sleep for correlation check."

    # 3. Check correlation: How many of those low-sleep days also had high tics?
    high_tic_on_low_sleep = low_sleep_days[low_sleep_days['tics'] >= HIGH_TICS]
    
    # If 70% or more of the low-sleep days result in high tics, vulnerability is confirmed.
    correlation_ratio = len(high_tic_on_low_sleep) / len(low_sleep_days)
    
    is_vulnerable = correlation_ratio >= 0.7 
    
    message = (
        f"Baseline check: {len(high_tic_on_low_sleep)} of {len(low_sleep_days)} low-sleep days had high tics. "
        f"Correlation Ratio: {correlation_ratio:.2f}. "
    )
    
    return is_vulnerable, message


def calculate_daily_metrics(data):
    """
    Processes the raw JSON data to calculate all normalized metrics, 
    the composite TNL score, separates custom impacts, and calculates sleep penalty.
    """
    df = pd.json_normalize(data)

    # Convert essential fields to numeric and handle missing values
    df['cognitive_load.study_minutes'] = pd.to_numeric(df['cognitive_load.study_minutes'], errors='coerce').fillna(0)
    df['emotional.stress'] = pd.to_numeric(df['emotional.stress'], errors='coerce').fillna(0)
    df['symptoms.tic_count'] = pd.to_numeric(df['symptoms.tic_count'], errors='coerce').fillna(0)
    df['physiological.sleep_hours'] = pd.to_numeric(df['physiological.sleep_hours'], errors='coerce').fillna(SLEEP_HOURS_THRESHOLD)
    
    # 1. Normalize Study Time (0-900 minutes -> 0-10 scale)
    df['normalized_study'] = (
        df['cognitive_load.study_minutes'].clip(upper=STUDY_MINUTES_MAX) / STUDY_MINUTES_MAX) * 10
    
    # 2. Extract Custom Factor Impacts
    df['positive_custom_impact'] = 0
    df['negative_custom_impact'] = 0

    def process_custom(row):
        pos_impact = 0
        neg_impact = 0
        if isinstance(row, list):
            for factor in row:
                level = factor.get('level', 0)
                effect = factor.get('effect', 0)
                impact = level * effect
                if impact > 0:
                    pos_impact += impact
                elif impact < 0:
                    neg_impact += impact
        return pos_impact, neg_impact

    df[['positive_custom_impact', 'negative_custom_impact']] = df['custom'].apply(
        lambda x: pd.Series(process_custom(x)))
    
    # 3. Conditional Sleep Penalty (Meta-Analysis Driven)
    is_vulnerable, _ = determine_sleep_vulnerability(df)
    
    df['sleep_penalty'] = 0
    if is_vulnerable:
        # Apply penalty only if sleep is below the threshold AND vulnerability is confirmed
        df['sleep_penalty'] = (SLEEP_HOURS_THRESHOLD - df['physiological.sleep_hours']).clip(lower=0) * SLEEP_PENALTY_WEIGHT

    # 4. Calculate Total Negative Load (TNL)
    df['total_negative_load'] = (
        df['emotional.stress'] +
        df['normalized_study'] +
        df['positive_custom_impact'] +
        df['sleep_penalty'] # Added only if confirmed vulnerability is true
    )
    
    # Prepare final DataFrame for clean plotting and analysis
    metrics_df = pd.DataFrame({
        'date': pd.to_datetime(df['date']),
        'tics': df['symptoms.tic_count'],
        'TNL': df['total_negative_load'],
        'stress_contrib': df['emotional.stress'],
        'study_contrib': df['normalized_study'],
        'pos_custom_contrib': df['positive_custom_impact'],
        'sleep_penalty_contrib': df['sleep_penalty'],
        'raw_neg_impact': df['negative_custom_impact'] # Keep raw value for plotting below zero
    })

    return metrics_df.set_index('date').sort_index()


def generate_pacing_recommendation(df):
    """
    Uses the TNL and Tic Count against the personalized 1-sigma rule 
    (Mean + 1 Std Dev) to generate an actionable recommendation.
    """
    if len(df) < 7:
        return {
            "pacing_state": "BASELINE NEEDED",
            "message": "Insufficient data (less than 7 days) to calculate personalized baseline. Continue tracking.",
            "latest_load": 0.0, "load_threshold": 0.0
        }

    latest_day = df.iloc[-1]
    baseline_df = df.iloc[-8:-1] if len(df) >= 8 else df.iloc[:-1]
    
    # Calculate Mean (mu) and Standard Deviation (sigma) for the baseline period
    mu_load = baseline_df['TNL'].mean()
    sigma_load = baseline_df['TNL'].std()
    mu_tics = baseline_df['tics'].mean()
    sigma_tics = baseline_df['tics'].std()

    # Define the 1-sigma spike threshold
    threshold_load = mu_load + sigma_load
    threshold_tics = mu_tics + sigma_tics

    # Check for spikes on the latest day
    load_spiking = latest_day['TNL'] > threshold_load
    tics_spiking = latest_day['tics'] > threshold_tics

    # --- Decision Matrix Logic ---
    if load_spiking and tics_spiking:
        state = "ADAPTIVE PACING ALERT"
        msg = (
            "⚠️ **ADAPTIVE PACING ALERT** for {}. Both your Total Negative Load ({:.1f}) and Tic Level ({:.0f}) are spiking. "
            "Recommendation: **Switch to Micro-Goals** immediately. Prioritize quick wins and recovery.".format(
                latest_day.name.strftime("%b %d"), latest_day['TNL'], latest_day['tics']
            )
        )
    elif load_spiking and not tics_spiking:
        state = "HIGH LOAD WARNING"
        msg = (
            "⚠️ **HIGH LOAD WARNING** for {}. Your Load ({:.1f}) is spiking, but symptoms are stable. "
            "Recommendation: **Preventative Rest.** You are coping well, but the underlying load is unsustainable. Schedule a mandatory break.".format(
                latest_day.name.strftime("%b %d"), latest_day['TNL']
            )
        )
    elif not load_spiking and tics_spiking:
        state = "UNUSUAL SPIKE"
        msg = (
            "💡 **UNUSUAL SPIKE** for {}. Tic Level ({:.0f}) is spiking, but your calculated Load is normal. "
            "Recommendation: **Re-Evaluate Custom Factors.** A new, untracked trigger (e.g., specific food or environment) may be at play. Track it now!".format(
                latest_day.name.strftime("%b %d"), latest_day['tics']
            )
        )
    else:
        state = "GREEN LIGHT"
        msg = (
            "✅ **GREEN LIGHT!** Your load and symptoms are stable and within the normal range. "
            "Recommendation: **Maintain Momentum.** Your current pacing and coping strategies are highly effective.".format(
                latest_day.name.strftime("%b %d")
            )
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
    fig, ax = plt.subplots(figsize=(12, 6)) 
    
    dates = df.index.strftime('%Y-%m-%d').tolist()
    
    # 1. Plot Positive Contributions (Stacked Bars, starting at 0)
    bottom = np.zeros(len(df))
    
    # Stack Stress (#FF8C42)
    stress_bars = ax.bar(dates, df['stress_contrib'], label='Stress (0-10)', color='#FF8C42', bottom=bottom)
    bottom += df['stress_contrib']
    
    # Stack Normalized Study (#43AA8B)
    study_bars = ax.bar(dates, df['study_contrib'], label='Cognitive Load', color='#43AA8B', bottom=bottom)
    bottom += df['study_contrib']
    
    # Stack Sleep Penalty (#B22222 - Firebrick/Reddish for severe penalty)
    sleep_bars = ax.bar(dates, df['sleep_penalty_contrib'], label='Sleep Deficit Penalty', color='#B22222', bottom=bottom)
    bottom += df['sleep_penalty_contrib']
    
    # Stack Positive Custom Impact (#2D7DD2)
    pos_custom_bars = ax.bar(dates, df['pos_custom_contrib'], label='Positive Custom Factor', color='#2D7DD2', bottom=bottom)
    
    # 2. Plot Negative Contributions (Below the axis)
    # This uses the raw negative impact values to plot below the zero line (#F5E663)
    neg_custom_bars = ax.bar(dates, df['raw_neg_impact'], label='Protective Custom Factor', color='#F5E663')

    # 3. Plot the Tic Count Trend (Line Plot)
    ax2 = ax.twinx()
    tic_line = ax2.plot(dates, df['tics'], color='r', marker='o', linestyle='-', linewidth=2, label='Tic Level (Trend)')

    # --- Axis and Label Configuration ---
    ax.set_title('MyFlow Daily Factor Contributions vs. Tic Level', fontsize=14)
    ax.set_xlabel('Date')
    
    # Left Y-Axis: Total Load Contributions
    ax.set_ylabel('Factor Contribution Score / Load Reduction', color='black')
    ax.set_ylim(min(0, df['raw_neg_impact'].min() - 1), max(bottom.max() + 1, df['tics'].max() / 2)) 
    ax.tick_params(axis='y', labelcolor='black')
    ax.axhline(0, color='grey', linewidth=1.0) # Zero line

    # Right Y-Axis: Tic Count
    ax2.set_ylabel('Tic Level (Target Symptom)', color='r')
    ax2.tick_params(axis='y', labelcolor='r')
    ax2.set_ylim(0, df['tics'].max() * 1.1) 

    # --- Combine and Place Legend ---
    bars = [stress_bars[0], study_bars[0], sleep_bars[0], pos_custom_bars[0], neg_custom_bars[0]]
    line_handle = tic_line[0]
    
    all_handles = bars + [line_handle]
    all_labels = [h.get_label() for h in all_handles]
    
    ax.legend(all_handles, all_labels, loc='upper center', bbox_to_anchor=(0.5, -0.15),
              ncol=3, fancybox=True, shadow=True, fontsize=8)

    # Clean up X-axis labels
    ax.set_xticks(range(len(dates)))
    ax.set_xticklabels([d[5:] for d in dates], rotation=45, ha='right', fontsize=8)

    plt.tight_layout(rect=[0, 0.15, 1, 1]) 
    
    # Save the plot and encode to Base64
    img_stream = io.BytesIO()
    plt.savefig(img_stream, format='png')
    plt.close(fig)
    img_stream.seek(0)
    
    img_base64 = base64.b64encode(img_stream.read()).decode('utf-8')
    return img_base64