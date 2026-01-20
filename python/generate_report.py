import requests
import json

def generate_full_report(json_file_path, output_html_path):
    with open(json_file_path, 'r') as f:
        data = json.load(f)
    
    # Call existing endpoints
    viz_response = requests.post('http://127.0.0.1:5000/api/visualization', json=data)
    viz_data = viz_response.json()
    
    sleep_response = requests.post('http://127.0.0.1:5000/api/sleep-analysis', json=data)
    sleep_data = sleep_response.json()
    
    # NEW: Call protective factors endpoint
    pf_response = requests.post('http://127.0.0.1:5000/api/protective-factors', json=data)
    pf_data = pf_response.json()
    
    # Extract visualization data
    graph_base64 = viz_data.get('graph_image_base64', '')
    pacing_message = viz_data.get('pacing_recommendation', '')
    pacing_state = viz_data.get('pacing_state', '')
    
    # Extract sleep data
    avg_sleep = sleep_data.get('avg_sleep_hours', 0)
    correlation = sleep_data.get('correlation_coefficient', 0)
    sleep_insight = sleep_data.get('insight_message', '')
    good_sleep_tics = sleep_data.get('avg_tics_good_sleep', 0)
    bad_sleep_tics = sleep_data.get('avg_tics_bad_sleep', 0)
    percent_diff = sleep_data.get('percent_difference', 0)
    
    # NEW: Extract protective factor data
    pf_insight = pf_data.get('insight_message', '')
    best_factor_name = pf_data.get('best_factor_name', 'N/A')
    best_factor_reduction = pf_data.get('best_factor_reduction', 0)
    best_factor_avg_with = pf_data.get('best_factor_avg_tics_with', 0)
    best_factor_avg_without = pf_data.get('best_factor_avg_tics_without', 0)
    lowest_tnl_date = pf_data.get('lowest_tnl_date', 'N/A')
    lowest_tnl_value = pf_data.get('lowest_tnl_value', 0)
    lowest_tnl_tics = pf_data.get('lowest_tnl_tics', 0)
    lowest_tnl_factors = pf_data.get('lowest_tnl_factors', [])
    all_factors = pf_data.get('all_factors', [])
    top_3_days = pf_data.get('top_3_best_days', [])
    
    # Set pacing color
    if 'GREEN' in pacing_state:
        pacing_color = '#4CAF50'
    elif 'WARNING' in pacing_state:
        pacing_color = '#FF9800'
    else:
        pacing_color = '#F44336'
    
    # Convert markdown bold to HTML
    pf_insight_html = pf_insight.replace('**', '<strong>').replace('<strong>', '</strong><strong>', 1)
    # Better approach - replace pairs
    import re
    pf_insight_html = re.sub(r'\*\*([^*]+)\*\*', r'<strong>\1</strong>', pf_insight)
    pf_insight_html = pf_insight_html.replace('\n\n', '</p><p class="insight" style="margin-top: 15px;">')
    pf_insight_html = pf_insight_html.replace('\n', '<br>')
    
    html = f"""<!DOCTYPE html>
<html>
<head>
<meta charset='UTF-8'>
<title>MyFlow Wellness Report</title>
<style>
body {{ font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }}
.container {{ max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }}
h1 {{ color: #333; border-bottom: 3px solid #2D7DD2; padding-bottom: 10px; }}
h2 {{ color: #555; margin-top: 30px; }}
.pacing-box {{ padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 5px solid {pacing_color}; background: {pacing_color}22; }}
.sleep-box {{ background: #E3F2FD; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 5px solid #2196F3; }}
.protective-box {{ background: #F3E5F5; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 5px solid #9C27B0; }}
.best-day-box {{ background: #E8F5E9; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 5px solid #4CAF50; }}
.stats {{ display: flex; justify-content: space-around; margin: 20px 0; flex-wrap: wrap; }}
.stat-card {{ background: #f9f9f9; padding: 15px; border-radius: 8px; text-align: center; min-width: 150px; margin: 10px; }}
.stat-value {{ font-size: 32px; font-weight: bold; color: #2D7DD2; }}
.stat-label {{ color: #666; font-size: 14px; margin-top: 5px; }}
img {{ max-width: 100%; height: auto; border-radius: 8px; margin: 20px 0; }}
.insight {{ font-size: 16px; line-height: 1.6; color: #444; }}
.legend {{ margin: 20px 0; padding: 15px; background: #f9f9f9; border-radius: 8px; }}
.legend-item {{ display: inline-block; margin-right: 20px; margin-bottom: 10px; }}
.legend-color {{ display: inline-block; width: 20px; height: 20px; border-radius: 3px; margin-right: 8px; vertical-align: middle; }}
.factor-ranking {{ margin: 15px 0; }}
.factor-item {{ display: flex; align-items: center; padding: 10px; margin: 8px 0; background: #fafafa; border-radius: 6px; border-left: 4px solid #9C27B0; }}
.factor-rank {{ font-size: 24px; margin-right: 15px; }}
.factor-details {{ flex: 1; }}
.factor-name {{ font-weight: bold; color: #333; font-size: 16px; }}
.factor-stats {{ color: #666; font-size: 13px; margin-top: 3px; }}
.reduction-badge {{ background: #4CAF50; color: white; padding: 3px 8px; border-radius: 12px; font-size: 12px; font-weight: bold; margin-left: 10px; }}
.best-days-grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-top: 15px; }}
.day-card {{ background: white; border: 2px solid #E8F5E9; border-radius: 8px; padding: 15px; text-align: center; }}
.day-card.gold {{ border-color: #FFD700; background: #FFFDE7; }}
.day-date {{ font-weight: bold; color: #333; font-size: 14px; }}
.day-tnl {{ font-size: 28px; font-weight: bold; color: #4CAF50; }}
.day-tics {{ color: #666; font-size: 13px; }}
.day-factors {{ font-size: 12px; color: #9C27B0; margin-top: 8px; }}
</style>
</head>
<body>
<div class='container'>

<h1>📊 Your MyFlow Wellness Report</h1>

<div class='pacing-box'>
<h2 style='margin-top: 0;'>{pacing_state}</h2>
<p class='insight'>{pacing_message}</p>
</div>

<h2>📈 What's Affecting Your Tics?</h2>
<p style='color: #666;'>This chart shows what makes your tics better or worse each day. Things above the line add stress. Things below the line help reduce it.</p>

<div class='legend'>
<div class='legend-item'><span class='legend-color' style='background: #FF8C42;'></span>😰 Stress Level</div>
<div class='legend-item'><span class='legend-color' style='background: #43AA8B;'></span>📚 Study Time</div>
<div class='legend-item'><span class='legend-color' style='background: #2D7DD2;'></span>😓 Things Making It Worse</div>
<div class='legend-item'><span class='legend-color' style='background: #F5E663;'></span>✨ Things Helping You</div>
<div class='legend-item'><span class='legend-color' style='background: #EE4266; width: 3px; height: 20px;'></span>📉 Your Tic Count</div>
</div>

<img src='data:image/png;base64,{graph_base64}' alt='MyFlow Chart'/>

<!-- ============================================ -->
<!-- NEW SECTION: What's Working FOR YOU -->
<!-- ============================================ -->
<h2>🛡️ What's Working FOR YOU</h2>
<p style='color: #666;'>These are YOUR best protective factors - the things that actually reduce YOUR tics based on YOUR data.</p>

<div class='protective-box'>
<p class='insight'>{pf_insight_html}</p>
</div>
"""

    # Add best factor stats if available
    if best_factor_name and best_factor_name != 'N/A':
        html += f"""
<div class='stats'>
<div class='stat-card' style='background: #F3E5F5;'>
<div class='stat-value' style='color: #9C27B0;'>🏆</div>
<div class='stat-label'><strong>{best_factor_name}</strong><br>Your #1 Protective Factor</div>
</div>

<div class='stat-card'>
<div class='stat-value' style='color: #4CAF50;'>{best_factor_reduction:.0f}%</div>
<div class='stat-label'>Tic Reduction<br>When You Use It</div>
</div>

<div class='stat-card'>
<div class='stat-value'>{best_factor_avg_with:.1f}</div>
<div class='stat-label'>Avg Tics WITH<br>{best_factor_name}</div>
</div>

<div class='stat-card'>
<div class='stat-value' style='color: #F44336;'>{best_factor_avg_without:.1f}</div>
<div class='stat-label'>Avg Tics WITHOUT<br>{best_factor_name}</div>
</div>
</div>
"""

    # Add ranking of all protective factors
    if all_factors and len(all_factors) > 0:
        html += """
<h3 style='color: #555; margin-top: 25px;'>📋 Your Protective Factor Ranking</h3>
<div class='factor-ranking'>
"""
        for i, factor in enumerate(all_factors[:5]):
            emoji = "🥇" if i == 0 else "🥈" if i == 1 else "🥉" if i == 2 else f"#{i+1}"
            reduction = factor.get('tic_reduction_pct', 0)
            badge_color = '#4CAF50' if reduction > 15 else '#FF9800' if reduction > 5 else '#9E9E9E'
            
            html += f"""
<div class='factor-item'>
<div class='factor-rank'>{emoji}</div>
<div class='factor-details'>
<div class='factor-name'>{factor.get('name', 'Unknown')}<span class='reduction-badge' style='background: {badge_color};'>{reduction:.0f}% reduction</span></div>
<div class='factor-stats'>Used {factor.get('times_used', 0)}x • Avg {factor.get('avg_tics_with', 0):.1f} tics with • {factor.get('avg_tics_without', 0):.1f} tics without</div>
</div>
</div>
"""
        html += "</div>"

    # Add best days section
   # Add best days section
    if top_3_days and len(top_3_days) > 0:
        html += """
<h2>🌟 Your Best Days (Lowest TNL)</h2>
<p style='color: #666;'>These days had the lowest Total Negative Load - here's what made them great!</p>
<div class='best-days-grid'>
"""
        # Track TNL values to handle ties
        prev_tnl = None
        prev_medal = None
        
        for i, day in enumerate(top_3_days[:3]):
            current_tnl = round(day.get('tnl', 0), 1)
            
            # Determine medal - same TNL = same medal
            if prev_tnl is not None and current_tnl == prev_tnl:
                medal = prev_medal  # Same as previous (tied!)
            else:
                medal = "🥇" if i == 0 else "🥈" if i == 1 else "🥉"
            
            prev_tnl = current_tnl
            prev_medal = medal
            
            card_class = "day-card gold" if medal == "🥇" else "day-card"
            factors_str = ", ".join(day.get('factors', [])) if day.get('factors') else "Natural good day!"
            
            html += f"""
<div class='{card_class}'>
<div class='day-date'>{medal} {day.get('date', 'N/A')}</div>
<div class='day-tnl'>{current_tnl}</div>
<div class='day-tics'>TNL Score • {day.get('tics', 0):.0f} tics</div>
<div class='day-factors'>✨ {factors_str}</div>
</div>
"""
        html += "</div>"

    # Sleep section (existing)
    html += f"""
<h2>😴 How Sleep Affects Your Tics</h2>
<div class='sleep-box'>
<p class='insight'>{sleep_insight}</p>
</div>

<div class='stats'>
<div class='stat-card'>
<div class='stat-value'>{avg_sleep:.1f}</div>
<div class='stat-label'>Hours of Sleep Per Night</div>
</div>

<div class='stat-card'>
<div class='stat-value'>{correlation:.2f}</div>
<div class='stat-label'>Sleep-Tic Connection<br><span style='font-size: 11px;'>(-1 = more sleep helps a lot!)</span></div>
</div>
"""

    if good_sleep_tics and bad_sleep_tics and percent_diff:
        html += f"""
<div class='stat-card'>
<div class='stat-value'>{percent_diff}%</div>
<div class='stat-label'>Fewer Tics With Good Sleep!</div>
</div>
</div>

<h2>😴 Good Sleep vs. Bad Sleep</h2>
<div class='stats'>
<div class='stat-card' style='background: #FFEBEE;'>
<div class='stat-value' style='color: #F44336;'>{bad_sleep_tics:.1f}</div>
<div class='stat-label'>Average Tics<br>(When You Sleep Under 6 Hours)</div>
</div>

<div class='stat-card' style='background: #E8F5E9;'>
<div class='stat-value' style='color: #4CAF50;'>{good_sleep_tics:.1f}</div>
<div class='stat-label'>Average Tics<br>(When You Sleep 7-9 Hours)</div>
</div>
</div>
"""
    else:
        html += "</div>"
    
    html += """
<p style='margin-top: 40px; color: #999; font-size: 12px; text-align: center;'>Generated by MyFlow Wellness Tracker</p>
</div>
</body>
</html>"""
    
    with open(output_html_path, 'w') as f:
        f.write(html)
    
    print(f"✅ Report generated: {output_html_path}")

if __name__ == '__main__':
    generate_full_report('test_data.json', 'final_test_report.html')