import requests
import json

def generate_full_report(json_file_path, output_html_path):
    with open(json_file_path, 'r') as f:
        data = json.load(f)
    
    viz_response = requests.post('http://127.0.0.1:5000/api/visualization', json=data)
    viz_data = viz_response.json()
    
    sleep_response = requests.post('http://127.0.0.1:5000/api/sleep-analysis', json=data)
    sleep_data = sleep_response.json()
    
    graph_base64 = viz_data.get('graph_image_base64', '')
    pacing_message = viz_data.get('pacing_recommendation', '')
    pacing_state = viz_data.get('pacing_state', '')
    
    avg_sleep = sleep_data.get('avg_sleep_hours', 0)
    correlation = sleep_data.get('correlation_coefficient', 0)
    sleep_insight = sleep_data.get('insight_message', '')
    good_sleep_tics = sleep_data.get('avg_tics_good_sleep', 0)
    bad_sleep_tics = sleep_data.get('avg_tics_bad_sleep', 0)
    percent_diff = sleep_data.get('percent_difference', 0)
    
    if 'GREEN' in pacing_state:
        pacing_color = '#4CAF50'
    elif 'WARNING' in pacing_state:
        pacing_color = '#FF9800'
    else:
        pacing_color = '#F44336'
    
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
.stats {{ display: flex; justify-content: space-around; margin: 20px 0; flex-wrap: wrap; }}
.stat-card {{ background: #f9f9f9; padding: 15px; border-radius: 8px; text-align: center; min-width: 150px; margin: 10px; }}
.stat-value {{ font-size: 32px; font-weight: bold; color: #2D7DD2; }}
.stat-label {{ color: #666; font-size: 14px; margin-top: 5px; }}
img {{ max-width: 100%; height: auto; border-radius: 8px; margin: 20px 0; }}
.insight {{ font-size: 16px; line-height: 1.6; color: #444; }}
.legend {{ margin: 20px 0; padding: 15px; background: #f9f9f9; border-radius: 8px; }}
.legend-item {{ display: inline-block; margin-right: 20px; margin-bottom: 10px; }}
.legend-color {{ display: inline-block; width: 20px; height: 20px; border-radius: 3px; margin-right: 8px; vertical-align: middle; }}
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