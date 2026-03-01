# MyFlow Python Microservice

> *The statistical analysis and visualization engine for MyFlow.*

A lightweight Flask server that receives daily log data from the Java desktop app and returns pacing recommendations, protective factor rankings, sleep-tic correlations, and chart images. Runs entirely on your local machine at **localhost:5000**.

---

## Endpoints

**POST /api/visualization**
The core endpoint. Calculates Total Negative Load (TNL) for each day, runs adaptive pacing detection, and returns a stacked bar chart as a base64 PNG.

Pacing states it can return:
- *GREEN LIGHT* — load and symptoms are stable
- *HIGH LOAD WARNING* — TNL spiking but symptoms okay
- *ADAPTIVE PACING ALERT* — both TNL and tics spiking
- *UNUSUAL SPIKE* — tics high but load looks normal

---

**POST /api/sleep-analysis**
Calculates the Pearson correlation between sleep hours and tic count. Finds your personal optimal sleep target by identifying what sleep amount corresponds to your lowest-tic days. Requires at least 7 days of data.

---

**POST /api/protective-factors**
Ranks all custom factors marked as protective (effect = -1) by how much they actually reduce tic count compared to your overall average baseline.

---

## Setup

**Step 1 — Install dependencies:**

    pip install flask numpy pandas matplotlib

**Step 2 — Start the server:**

    python app.py

Server starts at http://127.0.0.1:5000. Must be running before using the Diagnostics tab in the Java app.

---

## How the Math Works

**Total Negative Load (TNL)**
stress + normalized study hours + positive custom impact

Study minutes are clipped at 900 (15 hrs) and scaled to a 0–10 range to match the stress scale.

**Adaptive Pacing Threshold**
mean of last 7 days TNL + 1 standard deviation

Alerts only fire when the latest day exceeds *your* personal threshold — not a population average.

**Protective Factor Ranking**
% tic reduction = ((overall avg tics − avg tics on days with factor) / overall avg tics) × 100

Uses overall average as the baseline to avoid other high/low-load days skewing the comparison.

**Pearson Correlation (Sleep vs Tics)**
r = covariance(sleep, tics) / (std_sleep × std_tics)

A value close to −1 means more sleep strongly corresponds to fewer tics for this user. Implemented from scratch without scipy.

---

## Input Data Format

Each entry in the JSON array looks like this:

    {
      "date": "2024-11-15",
      "physiological": { "sleep_hours": 7.5 },
      "cognitive_load": { "study_minutes": 180 },
      "emotional": { "stress": 6 },
      "symptoms": { "tic_count": 3 },
      "screen": { "screen_time_hours": 2.0 },
      "social": { "social_conflict": false },
      "custom": [
        { "name": "Exercise", "level": 3, "effect": -1 },
        { "name": "Big exam", "level": 4, "effect": 1 }
      ],
      "journal": "Felt okay today."
    }

*effect: 1* = stress-adding factor

*effect: -1* = protective factor

---

## Dependencies

**flask**
HTTP server and routing

**pandas**
Data normalization and DataFrame operations

**numpy**
Array math for stacked bar chart

**matplotlib**
Chart generation (Agg backend — no display needed)

**statistics**
mean() for baseline calculations

**math**
sqrt() for Pearson correlation

---

## Report Generator

generate_full_report() at the bottom of app.py calls all three endpoints and assembles one self-contained HTML file. Used for the science fair final report output.

    python app.py