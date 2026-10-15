# Smart Solar Microgrid Trading System

A client-server application developed for the **SE4040 – Enterprise Application Development** module at **SLIIT**.

The Smart Solar Microgrid Trading System is designed to manage solar microgrid nodes, energy slots, prosumers, reservations, and energy transactions through a centralized RESTful Web API. The system consists of a React-based web application, a native Android mobile application, a C# Web API, and MongoDB.

---

## 📌 Project Overview

The system provides a centralized platform for managing energy trading activities within a smart solar microgrid environment.

The system supports three main user types:

* **Backoffice** – Manages users, prosumers, microgrid nodes, energy slots, and system administration.
* **Grid Operator** – Monitors bookings, manages operational activities, verifies transactions, and finalizes energy transfers.
* **Prosumer** – Registers and manages their account, views available energy slots, creates reservations, modifies/cancels bookings, and completes energy transactions.

All major business rules and data processing are handled through the centralized C# Web API.

---

## 🏗️ System Architecture

```text
                 ┌──────────────────────┐
                 │     React Web App    │
                 │   Backoffice /       │
                 │   Grid Operator      │
                 └──────────┬───────────┘
                            │
                         REST API
                            │
                            ▼
                 ┌──────────────────────┐
                 │    C# Web API        │
                 │  Centralized FAT     │
                 │  Business Logic      │
                 └──────────┬───────────┘
                            │
                            ▼
                 ┌──────────────────────┐
                 │      MongoDB         │
                 │   Central Database   │
                 └──────────────────────┘

                 ┌──────────────────────┐
                 │ Native Android App   │
                 │       Prosumer       │
                 │    Grid Operator     │
                 └──────────┬───────────┘
                            │
                         REST API
                            │
                            ▼
                 ┌──────────────────────┐
                 │    C# Web API        │
                 └──────────────────────┘

                 Android Local Storage
                         │
                         ▼
                    ┌─────────┐
                    │ SQLite  │
                    └─────────┘
```

### Architecture Principles

* Web and Android applications act as **UI/client layers**.
* All business logic is handled by the **central C# Web API**.
* Clients communicate with the server using **REST APIs**.
* Clients do not directly access MongoDB.
* MongoDB is used as the central NoSQL database.
* SQLite is used for required local Android persistence.
* The API follows the **FAT Service pattern**, where business logic is centralized in the service layer.

---

## 🚀 Main Features

### 🔐 Authentication & Authorization

* User login
* Role-based authentication
* Backoffice access
* Grid Operator access
* Prosumer authentication
* Role-based navigation
* Logout

### 👤 Prosumer Management

* Prosumer registration
* NIC-based identification
* Profile management
* Account modification
* Account deactivation request
* Pending account management
* Prosumer activation/deactivation/reactivation

### ⚡ Microgrid Node Management

* Create microgrid nodes
* Update node information
* GPS location management
* Capacity management
* Battery storage slot management
* Node schedule management
* Node deactivation
* Validation against active reservations

### 📅 Energy Slot & Reservation Management

* Create energy slots
* View available slots
* Update slots
* Manage battery slot availability
* Create energy reservations
* Modify reservations
* Cancel reservations
* Seven-day reservation validation
* Twelve-hour modification/cancellation validation
* Reservation status management
* Reservation summary

### 📱 Android Application

* Native Android application
* Prosumer mode
* Grid Operator mode
* SQLite local storage
* Booking dashboard
* Current and pending bookings
* Booking history
* Search and filtering
* Reservation management

### 🗺️ Google Maps Integration

* Display nearby microgrid nodes
* Show node locations
* Display node information
* Location-based station discovery

### 🔳 QR Transaction Verification

* Generate transaction QR codes
* Display secure transaction QR
* Scan QR using Android device
* Verify transaction information through the API
* Finalize energy transfer

### 🖥️ Web Application

* Backoffice dashboard
* Grid Operator dashboard
* User management
* Prosumer management
* Microgrid node management
* Energy slot management
* Reservation monitoring
* Booking search and filtering
* Operational monitoring

---

## 🛠️ Technologies Used

| Component             | Technology                     |
| --------------------- | ------------------------------ |
| Web Application       | React.js                       |
| Mobile Application    | Native Android                 |
| Mobile Language       | Java / Kotlin                  |
| Local Mobile Database | SQLite                         |
| Backend               | C# Web API                     |
| API Architecture      | REST                           |
| Database              | MongoDB                        |
| Web Server            | IIS                            |
| Maps                  | Google Maps API                |
| QR                    | Android QR scanning technology |
| Version Control       | Git & GitHub                   |

---


## 👥 Team Contributions


### Member 1 – Authentication & User Management

Responsibilities:

* Web authentication
* Role-based access
* Backoffice user management
* Prosumer registration
* Prosumer profile management
* Account activation/deactivation
* Pending account requests
* Authentication APIs
* User-related database operations
* Testing

### Member 2 – Microgrid Nodes & Energy Slots

Responsibilities:

* Microgrid node management
* GPS and capacity information
* Battery storage management
* Node schedules
* Node activation/deactivation
* Energy slot management
* Slot availability
* Node/slot APIs
* Database operations
* Business rule validation
* Testing

### Member 3 – Prosumer Reservations & Android Booking

Responsibilities:

* Native Android Prosumer interface
* SQLite local persistence
* Energy reservation creation
* Reservation modification
* Reservation cancellation
* Seven-day reservation rule
* Twelve-hour modification/cancellation rule
* Booking dashboard
* Booking history
* Search and filtering
* Reservation APIs
* Testing

### Member 4 – Grid Operator, QR & Maps

Responsibilities:

* Grid Operator web dashboard
* Booking monitoring
* Pending and approved reservation monitoring
* Energy availability monitoring
* Native Android Grid Operator mode
* QR code generation and scanning
* Transaction verification
* Energy transfer finalization
* Google Maps integration
* Operator-related APIs
* Testing

All members contributed to the integration of their respective modules with the centralized REST API.

---

## 🔗 API Communication

Both client applications communicate with the centralized API.

```text
React Web
     │
     │ HTTP / REST
     ▼
C# Web API
     │
     ▼
MongoDB
```

```text
Native Android
     │
     │ HTTP / REST
     ▼
C# Web API
     │
     ▼
MongoDB
```


---

## 💾 Android SQLite

SQLite is used for local Android persistence as required by the assignment.

Examples of locally stored information may include:

* User reference information
* NIC/reference ID
* User name
* User role
* Login/session-related information
* Required local reference data

Central reservation and transaction data remain managed by the Web API and MongoDB.

---

## 🔧 Setup & Installation

### Prerequisites

Install the following before running the system:

* Visual Studio
* .NET SDK
* IIS
* MongoDB / MongoDB Atlas
* Node.js and npm
* Android Studio
* Android SDK
* Java / JDK
* Git
* Google Maps API key

---

### 1. Clone the Repository

```bash
git clone <GITHUB_REPOSITORY_URL>
cd smart-solar-microgrid-trading-system
```

---

### 2. Configure the API

Open the API project in Visual Studio.

Configure:

* MongoDB connection string
* Database name
* API configuration
* Required environment settings

Then build and run the API.

For IIS deployment, publish the API and configure the IIS site/application according to the deployment configuration.

---

### 3. Configure the Web Application

Navigate to the Web project:

```bash
cd Web
npm install
```

Configure the API base URL in the web application's configuration.

Run:

```bash
npm start
```

---

### 4. Configure the Android Application

Open the `Mobile` project using Android Studio.

Configure:

* API base URL
* Google Maps API key
* Required Android permissions

Then build and run the application using an Android emulator or physical Android device.

---

## 🔑 Configuration



Use environment/configuration files for:

```text
MongoDB connection string
API URLs
Google Maps API key
Authentication configuration
```



---

## 🧪 Testing

Testing covers:

* Authentication
* Role-based access
* User management
* Prosumer management
* Microgrid node management
* Energy slot management
* Reservation validation
* Seven-day reservation rule
* Twelve-hour modification/cancellation rule
* API communication
* MongoDB operations
* Android SQLite persistence
* Google Maps integration
* QR scanning and verification
* Error handling
* Client-server integration

---


## 🎥 Demonstration Video

**Video Link:**
``



---

## 🔗 GitHub Repository

**Repository:**
``

---



## ⚠️ Academic Project

This project was developed as part of the **SE4040 – Enterprise Application Development** module at the **Sri Lanka Institute of Information Technology (SLIIT)**.

**Academic Year:** 2026
**Degree:** BSc (Hons) Information Technology – Software Engineering
**Year:** 4
**Semester:** 2

---


## 📜 License

This project is developed for academic purposes as part of the SE4040 Enterprise Application Development module.
