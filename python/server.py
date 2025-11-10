# app.py
# Python AI Visualization Microservice for the MyFlow Project
# This Flask service accepts personalized JSON data via POST,
# calculates the Total Negative Load (TNL) and Adaptive Pacing recommendation,
# and returns a Matplotlib chart encoded in Base64.

import io
import base64
import matplotlib
matplotlib.use('Agg')  # Use non-interactive backend
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from flask import Flask, request, jsonify
# The following line is needed if you are running this service locally
# from flask_cors import CORS

app = Flask(__name__)
# CORS(app) # Uncomment this line if you encounter CORS issues running locally

# --- HELPER FUNCTIONS ---

def calculate_daily_metrics(data):
    """
    Processes the raw JSON data to calculate normalized metrics,
    the composite TNL score, and separates custom impacts.
    """
    df = pd.json_normalize(data)

    # 1. Normalize Study Time (0-180 minutes -> 0-10 scale)
    # Handle potential missing or non-numeric values
    df['cognitive_load.study_minutes'] = pd.to_numeric(
        df['cognitive_load.study_minutes'], errors='coerce').fillna(0)
    
    # Cap at 900 minutes for the normalization base
    df['normalized_study'] = (
        df['cognitive_load.study_minutes'].clip(upper=900) / 900) * 10
    
    # 2. Extract Custom Factor Impacts
    # Initialize custom impact columns
    df['positive_custom_impact'] = 0
    df['negative_custom_impact'] = 0

    # Process the custom factors array for each day
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

    # Apply the custom factor processing across the DataFrame
    df[['positive_custom_impact', 'negative_custom_impact']] = df['custom'].apply(
        lambda x: pd.Series(process_custom(x)))
    
    # 3. Calculate Total Negative Load (TNL)
    # Stress is already on a 0-10 scale
    df['emotional.stress'] = pd.to_numeric(df['emotional.stress'], errors='coerce').fillna(0)
    
    df['total_negative_load'] = (
        df['emotional.stress'] +
        df['normalized_study'] +
        df['positive_custom_impact']
    )
    
    # Prepare columns for plotting and analysis
    metrics_df = pd.DataFrame({
        'date': pd.to_datetime(df['date']),
        'tics': pd.to_numeric(df['symptoms.tic_count'], errors='coerce').fillna(0),
        'TNL': df['total_negative_load'],
        'stress_contrib': df['emotional.stress'],
        'study_contrib': df['normalized_study'],
        'pos_custom_contrib': df['positive_custom_impact'],
        # Negative custom is plotted separately, using the absolute value for stacking
        'neg_custom_contrib': df['negative_custom_impact'].abs(), 
        'raw_neg_impact': df['negative_custom_impact'] # Keep raw value for analysis
    })

    return metrics_df.set_index('date').sort_index()


def generate_pacing_recommendation(df):
    """
    Uses the Mean + 1 Standard Deviation (1-sigma rule) to determine if the 
    latest day is a 'spike' and generates an adaptive pacing recommendation.
    """
    # Requires at least 7 days of history for meaningful comparison
    if len(df) < 7:
        return {
            "pacing_state": "Baseline Needed",
            "message": "Continue tracking data for 7 days to establish your personalized baseline for Adaptive Pacing."
        }

    # Get the latest day's data
    latest_day = df.iloc[-1]
    
    # Get the preceding 7 days (or less if the dataset is smaller than 7)
    baseline_df = df.iloc[-8:-1] if len(df) >= 8 else df.iloc[:-1]
    
    # Calculate Mean (mu) and Standard Deviation (sigma) for the baseline period
    mu_load = baseline_df['TNL'].mean()
    sigma_load = baseline_df['TNL'].std()
    
    mu_tics = baseline_df['tics'].mean()
    sigma_tics = baseline_df['tics'].std()

    # Define the spike threshold (Mean + 1 Std Dev)
    threshold_load = mu_load + sigma_load
    threshold_tics = mu_tics + sigma_tics

    # Check for spikes on the latest day
    load_spiking = latest_day['TNL'] > threshold_load
    tics_spiking = latest_day['tics'] > threshold_tics

    # --- Decision Matrix Logic ---
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
            "Recommendation: **Maintain Momentum.** Your current pacing and coping strategies are effective. Continue with your scheduled goals.".format(
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
    # Set up the plot aesthetics
    plt.style.use('ggplot')
    fig, ax = plt.subplots(figsize=(10, 5))
    
    dates = df.index.strftime('%Y-%m-%d').tolist()
    
    # 1. Plot Positive Contributions (Stacked Bars, starting at 0)
    bottom = np.zeros(len(df))
    
    # Stack Stress
    stress_bars = ax.bar(dates, df['stress_contrib'], label='Stress (0-10)', color='#FF8C42', bottom=bottom)
    bottom += df['stress_contrib']
    
    # Stack Normalized Study
    study_bars = ax.bar(dates, df['study_contrib'], label='Cognitive Load (Normalized)', color='#43AA8B', bottom=bottom)
    bottom += df['study_contrib']
    
    # Stack Positive Custom Impact
    pos_custom_bars = ax.bar(dates, df['pos_custom_contrib'], label='Positive Custom Impact', color='#2D7DD2', bottom=bottom)
    
    # 2. Plot Negative Contributions (Below the axis)
    # Plot Negative Custom Impact (e.g., Music Break, Vacation Day)
    # This is plotted directly using the negative/absolute contribution values
    neg_custom_bars = ax.bar(dates, df['raw_neg_impact'], label='Protective Custom Factor', color='#F5E663')

    # 3. Plot the Tic Count Trend (Line Plot)
    # Create a secondary axis for Tic Count (The Target Symptom)
    ax2 = ax.twinx()
    tic_line = ax2.plot(dates, df['tics'], color='#EE4266', marker='o', label='Tic Count (Trend)')

    # --- Axis and Label Configuration ---
    ax.set_title('MyFlow Daily Factor Contributions vs. Tic Count', fontsize=14)
    ax.set_xlabel('Date')
    
    # Left Y-Axis: Total Load Contributions
    ax.set_ylabel('Factor Contribution Score / Load Reduction', color='black')
    ax.tick_params(axis='y', labelcolor='black')
    ax.axhline(0, color='grey', linewidth=0.8) # Zero line

    # Right Y-Axis: Tic Count
    ax2.set_ylabel('Tic Count (Target Symptom)', color='#EE4266')
    ax2.tick_params(axis='y', labelcolor='#EE4266')
    
    # Combine legends
    bars = [stress_bars[0], study_bars[0], pos_custom_bars[0], neg_custom_bars[0]]
    labels = [b.get_label() for b in bars]
    labels.append(tic_line[0].get_label())
    
    ax.legend(bars + tic_line, labels, loc='upper left', fontsize=8)

    # Clean up X-axis labels
    ax.set_xticks(range(len(dates)))
    ax.set_xticklabels([d[5:] for d in dates], rotation=45, ha='right', fontsize=8)

    plt.tight_layout()
    
    # Save the plot to a BytesIO object
    img_stream = io.BytesIO()
    plt.savefig(img_stream, format='png')
    plt.close(fig)
    img_stream.seek(0)
    
    # Encode to Base64
    img_base64 = base64.b64encode(img_stream.read()).decode('utf-8')
    return img_base64

# --- FLASK APPLICATION ENDPOINT ---

@app.route('/api/visualization', methods=['POST'])
def get_visualization():
    """
    Receives JSON data, runs analysis, and returns the result.
    """
    print("HOLA")
    if not request.json:
        return jsonify({"error": "Missing JSON payload"}), 400

    data = request.json
    
    try:
        # 1. Calculate Metrics and Analyze
        metrics_df = calculate_daily_metrics(data)
        
        # 2. Generate Adaptive Pacing Recommendation
        recommendation = generate_pacing_recommendation(metrics_df)
        
        # 3. Generate Visualization (Stacked Bar Chart)
        img_base64 = generate_visualization(metrics_df)

        # 4. Return combined JSON response
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

# --- SERVER STARTUP ---

if __name__ == '__main__':
    print("\n" + "="*50)
    print("🚀 Starting MyFlow Python AI Microservice (Flask)")
    print("="*50)
    print("API Endpoint: POST /api/visualization")
    print("Server running on http://127.0.0.1:5000")
    print("="*50 + "\n")
    # Use 0.0.0.0 for broader access if needed, e.g., in a container
    app.run(debug=True, port=5000, host='127.0.0.1')

