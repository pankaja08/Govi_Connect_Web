# 🌾 Govi CONNECT : Smart Agriculture Platform & AI-Based Diagnostic System

<div align="center">
  <img src="https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJpbmZ6Z3R6bmZ6Z3R6bmZ6Z3R6bmZ6Z3R6bmZ6Z3R6bmZ6Z3R6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/LpALgGQNZLz9S896vU/giphy.gif" width="600" alt="Smart Agriculture Animation">
</div>

[![Project Status](https://img.shields.io/badge/Project%20Status-Active-brightgreen)](#)
[![Tech Stack](https://img.shields.io/badge/Tech%20Stack-Spring%20Boot%20%7C%20FastAPI%20%7C%20MySQL-blue)](#)
[![Project ID](https://img.shields.io/badge/Project%20ID-DS--01--G11-orange)](#)

**GoSCONNECT** (Govi Connect) is an "all-in-one" digital ecosystem specifically designed to empower small-holder farmers in Sri Lanka. By integrating advanced AI diagnostics with a direct-to-consumer marketplace and expert guidance, the platform aims to reduce crop wastage and enhance financial stability.

---

## 🚀 Key Modules & Features

The platform is divided into specialized modules to address every step of the farming lifecycle:

### 1. 🔍 AI Disease Diagnostic Suite
* **Intelligent Identification:** Uses **CNN (ResNet)** architectures to provide instant, data-driven diagnostics for paddy leaf diseases.
* **Disease Coverage:** Detects conditions like Bacterial Leaf Blight, Rice Blast, and Brown Spot.
* **Actionable Advice:** Maps AI outputs to step-by-step treatment and prevention instructions.

### 2. 🛒 GOD Mart (Marketplace)
* **Direct Access:** A transparent platform where farmers sell products directly to consumers at fair prices.
* **Management:** Features product listing, category browsing, and a review/rating system to build trust.

### 3. 🛡️ Smart Crop Advisory System
* **Data-Driven Decisions:** Provides recommendations on suitable crops based on climate zone, season, and soil type.
* **Care Instructions:** Offers fertilizer suggestions and basic crop care guidelines to minimize risks.

### 4. 💬 Community Forum & Expert Support
* **Knowledge Sharing:** A space for farmers to ask questions and share practices with peers.
* **AI Assistance:** Features **Gemini API** integration to suggest quick responses for common agricultural queries.

### 5. 📅 Personal Farming Workspace
* **Activity Tracking:** A digital log and calendar for farmers to schedule and track daily tasks and seasonal milestones.

---

## 🛠️ Tech Stack

**GoSCONNECT** is built using modern, scalable, and open-source technologies:

* **Backend:** Java (Spring Boot) & Python (FastAPI/Flask for AI modules)
* **Database:** MySQL (Relational Management)
* **AI/ML:** CNN (ResNet) for image analysis & Gemini API for LLM features
* **Frontend:** HTML5, CSS3, JavaScript (ES6+)
* **Security:** Spring Security (Role-Based Access Control)

---

## 👥 Meet the Team (Group DS-01-G11)

| Registration No. | Name | Primary Responsibility |
| :--- | :--- | :--- |
| **IT24102543** | **Bawanya S.A.S.** | Admin Management & System Governance |
| **IT24103504** | **Yunidu E.D.P** | Content Management System (Knowledge Hub) |
| **IT24102361** | **Sharanjan.S** | MarketPlace Management (GOD Mart) |
| **IT24102310** | **Jayawardena H.S.P.P** | Smart Crop Advisory System |
| **IT24103260** | **Udayanga G.W.A** | User & Personal Farming Profile Management |
| **IT24102555** | **Weerathunga B.A** | Discussion Forum & Community Support |

---

## ⚙️ Installation & Setup

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/YourUsername/GoSCONNECT.git](https://github.com/YourUsername/GoSCONNECT.git)
    ```
2.  **Database Setup:**
    * Create a MySQL database named `gosconnect_db`.
    * Update `application.properties` with your MySQL credentials.
3.  **Backend (Spring Boot):**
    * Run `./mvnw spring-boot:run`
4.  **AI Module (FastAPI):**
    * Navigate to the `/ai-module` folder.
    * Run the server: `uvicorn main:app --reload`

---

*Developed for the IT2021 AIML Project - SLIIT Malabe Campus (2026)*
