# Thrift It

A **buy & sell marketplace app** focused on **students and local communities**.  
Thrift It helps users list unused items and discover nearby deals within their **university, hostel, or organization**.

Built with **Kotlin + Jetpack Compose**, designed around an **offline-first architecture** for reliability even on unstable networks.

---

## ✨ Features

- Phone number authentication (OTP based)
- Profile setup with location
- Buy & sell items nearby
- Offline-first experience
- Clean, minimal UI with Material 3
- WhatsApp-based seller contact
- Designed for students & closed communities

---

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM + Offline-First
- **Backend**: Firebase (Auth, Firestore)
- **Local Cache**: Room Database
- **Image Loading**: Coil
- **DI**: Hilt

---

## 🎯 Roadmap

### **v0.1.0 – Initial Release**

#### Authentication & Profile
- [x] Phone number authentication (OTP)
- [x] Auth persistence across app restarts
- [x] Profile setup (name, phone, location)
- [x] Location permission handling
- [x] Session management (login / logout)

#### Buy Flow
- [x] Browse items in grid layout
- [x] Item card with image, price, description, distance
- [x] Search items by keyword
- [x] Filter by price and distance
- [x] Pull-to-refresh support
- [x] Empty & loading states
- [x] Item detail view
- [x] WhatsApp deep link to contact seller
- [ ] Fetch seller phone dynamically from Firestore

#### Sell Flow
- [x] Pick images from gallery
- [x] Capture image via camera
- [x] Image preview & removal
- [x] Item form (name, price, description, condition)
- [x] Form validation
- [x] Upload item with images
- [x] Upload success notification
- [ ] Clear form after upload
- [ ] Auto-navigate to Buy screen after upload
- [ ] Item category selection

#### Offline-First
- [x] Room as single source of truth
- [x] Firestore offline persistence
- [x] Background sync with Firestore
- [x] Network connectivity monitoring
- [x] Offline upload queue
- [ ] Pending upload indicator in UI
- [ ] Reliable re-sync when network restores

#### UI & UX
- [x] Compose-based UI for all screens
- [x] Bottom navigation (Buy, Sell, Settings)
- [x] Top app bar with network indicator
- [x] Filter bottom sheet
- [x] Item detail dialog
- [x] Notification screen UI
- [x] Settings screen UI
- [x] Custom theme & typography
- [x] Animated splash screen
- [x] Adaptive app icon

---

### **v0.2.0 – Upcoming**

#### Features
- [ ] Pagination for Buy screen
- [ ] Edit profile support
- [ ] Update location from Settings
- [ ] Clear local cache on sign out
- [ ] Category-based item browsing
- [ ] Pending upload indicator
- [ ] Retry actions for failed uploads

#### UX Improvements
- [ ] Item card design polish
- [ ] Improved spacing & elevation
- [ ] Skeleton loading states
- [ ] Better error messages & retry UI
- [ ] Dark mode fine-tuning

#### Quality & Stability
- [ ] Extract hardcoded strings
- [ ] UI testing for core flows
- [ ] Test offline scenarios thoroughly
- [ ] Test across Android versions
- [ ] Performance optimizations

---

## 📱 Screenshots

> Screenshots and GIFs will be added as the UI continues to evolve.

---

## 🚀 Project Status

This project is **actively developed** and serves as:
- A real-world **offline-first Compose app**
- A **student-focused marketplace**
- A long-term, production-quality Android project
