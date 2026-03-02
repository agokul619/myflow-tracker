# MyFlow : Wellness & Tic Tracker

> *Your patterns. Your triggers. Your solutions.*

MyFlow is a desktop app for tracking your personal health that is made for people who want to know what really makes their symptoms worse. It gives you personalized advice based on your data and patterns. It tracks daily stress, sleep, study load, and custom personal factors, then uses statistical analysis to give you adaptive pacing alerts and personalized insights.

---

## What It Does

MyFlow has two core parts:

**1. Daily Check-In (Java Desktop App)**
Log how you're feeling every day — stress level, tic count, hours of sleep, study time, screen time, and anything custom like "went for a walk" or "had a fight with someone". You can navigate back to previous days to fill in missed entries.

**2. Pattern Analysis (Python Microservice)**
Once you have at least 7 days of data, MyFlow sends it to a local Python/Flask server that runs three types of analysis:

- *Adaptive Pacing Alerts* — detects if your Total Negative Load (TNL) or tic count is spiking above your personal baseline and tells you exactly what to do
- *Protective Factor Ranking* — figures out which of your custom factors actually reduces your tics the most, ranked by real percentage reduction
- *Sleep-Tic Correlation* — calculates a Pearson correlation between your sleep hours and tic count, finds your personal optimal sleep target

Results are rendered as a full HTML report that opens right inside the app.

---

## Architecture

The project is split into two independent services that talk to each other over localhost:

**Java Desktop App** → collects data, saves to JSON, sends to Python for analysis

**Python Flask Server** → receives JSON, runs statistics, returns results + chart image

---

## Project Structure

**MyFlowApp.java**
Main window, two-tab layout

**LoginScreen.java**
Login + entry point

**RegistrationScreen.java**
New user account creation

**DailyLogPanel.java**
Daily check-in form (Tab 1)

**DiagnosticsPanel.java**
Analysis results viewer (Tab 2)

**DataManager.java**
JSON read/write, singleton pattern

**AnalysisClient.java**
HTTP client that calls Python API

**DailyLog.java**
Core data model

**CustomFactor.java**
Personalized factor model

**python/app.py**
Flask server, all 3 API endpoints

**myflow_data.json**
Your log data, auto-created

**reports/**
Generated HTML reports

The Java app and Python service communicate over **localhost:5000**. You must start the Python server before running analysis.

---

## Getting Started

**Java requirements:**
- Java 11 or higher
- Eclipse IDE or any Java IDE
- json-20231013.jar — included in the project root, add to Build Path

**Python requirements:**
- Python 3.8 or higher
- Install dependencies:

    pip install flask numpy pandas matplotlib

---

## Running the App

**Step 1 — Start the Python microservice first:**

    cd python
    python app.py

You should see:

    🚀 Starting MyFlow Python AI Microservice (Flask)
    Server running on http://127.0.0.1:5000

**Step 2 — Run the Java app:**

In Eclipse: right-click LoginScreen.java → *Run As* → *Java Application*

**Step 3 — Create an account and start logging.**

---

## How to Use It

**1.** Log daily using the "Today's Check-In" tab — takes about 2 minutes

**2.** Add custom factors for things unique to your life like "therapy session" or "exercise" — mark them as stress-adding (+1) or protective (-1)

**3.** After 7+ days, go to the "My Patterns" tab → select a date range → click *Run Diagnostics*

**4.** Your personalized HTML report appears in the app with pacing alerts, protective factor rankings, and sleep analysis

---

## The Statistics Behind It

**Total Negative Load (TNL)**
stress + normalized study hours + positive custom impact

**Adaptive Pacing Threshold**
mean of last 7 days + 1 standard deviation

**Protective Factor Ranking**
% tic reduction vs. overall average baseline

**Sleep-Tic Correlation**
Pearson correlation coefficient (r)

The 1-sigma threshold means alerts only fire when something is genuinely unusual for *you* — not based on population averages.

---

## Data Storage

All your data lives locally in myflow_data.json — a JSON array of daily log entries. Nothing is sent to any external server. The Python service runs entirely on your own machine at 127.0.0.1.

A backup is saved to the backups/ folder whenever you want via DataManager.createBackup().

---

## Project Status

Built as a personal science fair project for Synopsys Science Fair. Actively developed. Known areas for future improvement:

- Multi-user data isolation (currently all users share one data file)
- Password hashing (currently plaintext in .properties file)
- Swing to JavaFX UI upgrade

---

## Tech Stack

**Desktop UI**
Java Swing

**Data persistence**
JSON via org.json library

**HTTP client**
java.net.http.HttpClient (Java 11+)

**Analysis server**
Python + Flask

**Data processing**
pandas, numpy

**Visualization**
matplotlib

**Statistics**
Custom Pearson implementation