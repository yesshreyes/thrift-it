# Thrift It - Development Roadmap & Checklist

**Project**: Buy & Sell Marketplace App  
**Tech Stack**: Kotlin, Jetpack Compose, Firebase
**Architecture**: Offline-First MVVM  

---

## 📋 Phase 1: Project Setup & Basic UI 

### Project Setup
- [x] GitHub repo initial commit
- [x] Add README.md
- [x] Setup Ktlint
- [x] Update versions

### Screen Structure Setup
- [x] Create `MainActivity.kt` with theme setup
- [x] Create `AuthScreen.kt` (UI only)
- [x] Create `ProfileSetupScreen.kt` (UI only)
- [x] Create `BuyScreen.kt` (UI only - grid layout)
- [x] Create `SellScreen.kt` (UI only - form layout)
- [x] Create `SettingsScreen.kt` (UI only - list layout)
- [x] Create `NotificationScreen.kt` (UI only)

### Navigation Setup
- [x] Create `NavGraph.kt` with all routes defined
- [x] Implement bottom navigation bar (Buy, Sell, Settings)
- [x] Add navigation between screens
- [x] Add top bar with "THRIFT IT" branding
- [x] Add notification icon in top bar

### Static UI Components
- [x] Design item card component for grid view
- [x] Create filter bottom sheet UI
- [x] Create search bar component
- [x] Create item detail dialog/bottom sheet
- [x] Create upload form UI

---

## 📋 Phase 2: Data Layer & Backend Setup 

### Firebase Project Setup
- [x] Create Firebase project in console
- [x] Enable Phone Authentication in Firebase Console
- [x] Create Firestore database
- [x] Setup Firestore collections structure:
  - [x] `users` collection
  - [x] `items` collection
- [x] Configure Firestore indexes for queries

### Image Storage
- [x] Create Cloudinary account
- [x] Get Cloud Name, API Key, and API Secret from dashboard
- [x] Add Cloudinary credentials to Android project

### Room Database Setup
- [x] Create `entities/ItemEntity.kt` with all fields
- [x] Create `entities/UserEntity.kt` with all fields
- [x] Create `dao/ItemDao.kt` with CRUD operations
- [x] Create `dao/UserDao.kt` with CRUD operations
- [x] Create `AppDatabase.kt` with Room configuration
- [x] Add database migration strategy

### Data Models
- [x] Create `models/Item.kt` (domain model)
- [x] Create `models/User.kt` (domain model)
- [x] Create mapper functions (Entity ↔ Domain Model)
- [x] Create `sealed class Result<T>` for API responses
- [x] Create `sealed class UiState<T>` for UI states

### Repository Layer
- [x] Create `repository/AuthRepository.kt`
  - [x] Phone authentication methods
  - [x] User profile CRUD
- [x] Create `repository/ItemRepository.kt`
  - [x] Fetch items from Firestore
- [x] Create `repository/UserRepository.kt`
  - [x] User profile management
  - [x] Location updates
- [x] Create `repository/UploadRepository.kt`
  - [x] Image upload to Cloudinary
  - [x] Item creation in Firestore

### ViewModels Setup
- [x] Create `viewmodel/AuthViewModel.kt`
  - [x] Phone auth state management
  - [x] OTP verification logic
- [x] Create `viewmodel/BuyViewModel.kt`
  - [x] Items flow from repository
  - [x] Search state
  - [x] Filter state
- [x] Create `viewmodel/SellViewModel.kt`
  - [x] Upload state management
  - [x] Form validation
- [x] Create `viewmodel/SettingsViewModel.kt`
  - [x] User settings state
  - [x] Sign out logic
- [x] Create `viewmodel/ProfileViewModel.kt`
  - [x] Profile setup state

---

## 📋 Phase 3: Core Features - Authentication 

### Phone Authentication
- [x] Implement Firebase Phone Auth in `AuthRepository`
- [x] Create OTP input UI in `AuthScreen`
- [x] Handle OTP verification
- [x] Show loading/error states
- [x] Navigate to profile setup on success
- [x] Store auth state in ViewModel
- [x] Handle auth persistence

### Profile Setup
- [x] Request location permission using Accompanist
- [x] Get user's current location using FusedLocationProvider
- [x] Create profile form (name, address, phone)
- [x] Upload profile data to Firestore `/users/{userId}`
- [x] Navigate to main app on completion
- [x] Handle permission denied scenarios

### Session Management
- [x] Check if user is already logged in on app start
- [x] Navigate accordingly (Auth vs Main)
- [x] Implement sign out functionality

---

## 📋 Phase 4: Core Features - Buy Screen 

### Data Fetching
- [x] Fetch items from Firestore in `ItemRepository`
- [x] Expose items from repository to ViewModel
- [x] Update UI using StateFlow in Compose
- [x] Handle empty state (no items)
- [x] Handle loading state
- [x] Handle Firestore errors

### Grid Layout Implementation
- [x] Implement `LazyVerticalGrid` with 2 columns
- [x] Display item cards with:
  - [ ] Image (using Coil)
  - [x] Name
  - [x] Price
  - [x] Description
  - [x] Distance from user
- [x] Calculate distance using Haversine formula
- [x] Format distance (e.g., "2.5 km away")

### Search Functionality
- [x] Add search bar in top bar
- [x] Implement search logic in repository
- [x] Filter items by keywords
- [x] Update UI with search results
- [x] Handle empty search results
- [x] Add clear search button

### Filter Functionality
- [x] Create filter bottom sheet UI
- [x] Add price range slider
- [x] Add distance range slider
- [x] Apply filters to Firestore query
- [x] Update UI with filtered results
- [x] Show active filters indicator
- [x] Add clear filters option

### Item Detail & WhatsApp Integration
- [x] Create item detail dialog/bottom sheet
- [x] Show full item details on card click
- [x] Add "Connect" button
- [ ] Get seller's phone from Firestore
- [x] Create WhatsApp deep link with pre-filled message
- [x] Launch WhatsApp with Intent
- [x] Handle WhatsApp not installed case

---

## 📋 Phase 5: Core Features - Sell Screen 

### Image Picker
- [x] Request camera permission
- [x] Implement `ActivityResultContracts.PickVisualMedia()`
- [x] Add option to pick from gallery
- [x] Add option to take photo
- [x] Show selected image preview
- [x] Handle permission denied
- [x] Compress image before upload

### Upload Form
- [x] Create form fields:
  - [x] Item name (TextField)
  - [x] Price (TextField with number input)
  - [x] Description (TextField multiline)
  - [x] Item condition (Dropdown/Radio)
- [x] Add form validation
- [x] Show validation errors
- [x] Disable submit until valid

### Firebase Upload
- [x] Upload image to Firebase Storage
- [x] Get download URL
- [x] Create item document in Firestore with:
  - [x] Image URL
  - [x] User ID (seller)
  - [x] Location (lat/lng)
  - [x] Timestamp
  - [x] All form data
- [x] Show upload progress
- [x] Handle upload errors
- [x] Show success message
- [ ] Clear form after successful upload
- [ ] Navigate to Buy screen to see uploaded item
- [ ] Category Selection

---

## 📋 Phase 6: Core Features - Settings & Notifications 

### Settings Screen
- [x] Display user profile info
- [ ] Add edit profile option
- [x] Add update location button
- [ ] Update location in Firestore and Room
- [x] Show app version
- [x] Add sign out button
- [ ] Clear all data on sign out

### Notifications
- [x] Create notification channel
- [x] Show success notification when uploaded
- [x] Add notification permission request (Android 13+)
- [x] Notification should open app on click

---

## 📋 Phase 7: Offline-First Implementation

### Offline Architecture
- [x] Make Room the single source of truth
- [x] Always read from Room, not Firestore directly
- [x] Set Firestore offline persistence enabled
- [x] Implement repository pattern properly:
  - [x] Emit data from Room
  - [x] Sync Firestore in background
  - [x] Update Room on Firestore changes
- [x] Clear Room cache on sign out
- [x] Cache user data in Room

### Network State Handling
- [x] Create `NetworkObserver` to monitor connectivity
- [x] Show network status indicator in UI
- [x] Queue uploads when offline
- [x] Sync when back online

### Offline Upload Queue
- [x] Store pending uploads in Room
- [x] Mark items as "pending_upload"
- [x] Update status on success
- [ ] Show pending indicator in UI
- [ ] Fix sync after network available

### Pull to Refresh
- [x] Add `SwipeRefresh` to Buy screen
- [x] Trigger Firestore fetch on pull
- [x] Update Room with new data
- [x] Show refresh indicator

### Pagination
- [ ] Pagination logic for Buy and other screens
- [ ] Handle sync conflicts

---

## 📋 Phase 8: UI/UX Polish & Beautification (Week 6)

### Splash Screen
- [ ] Create animated splash screen
- [ ] Add "THRIFT IT" logo
- [ ] Add brand colors
- [ ] Show for 2 seconds
- [ ] Navigate based on auth state

### App Icon
- [ ] Design app icon
- [ ] Create adaptive icon (foreground + background)
- [ ] Generate all required sizes
- [ ] Update manifest with icon
- [ ] Test on different launchers

### Theme & Branding
- [ ] Define color palette (primary, secondary, accent)
- [ ] Create Material3 theme
- [ ] Use consistent typography

### UI Improvements
- [ ] Polish item card design
- [ ] Improve spacing and padding
- [ ] Add subtle shadows and elevations
- [ ] Improve button styles
- [ ] Add icons where appropriate
- [ ] Ensure proper contrast ratios
- [ ] Test on different screen sizes

---

## 📋 Phase 9: Testing & Optimization 

### Functional Testing
- [ ] Test complete auth flow
- [ ] Test offline scenarios
- [ ] Test pagination behavior
- [ ] Test search and filters
- [ ] Test image upload
- [ ] Test WhatsApp integration
- [ ] Test location permission flow
- [ ] Test sign out and data clearing
- [ ] Test on different Android versions

### Code Quality
- [ ] Extract hardcoded strings to resources
- [ ] Use constants for magic numbers
- [ ] Refactor duplicate code
- [ ] Ensure proper package structure

### Documentation
- [ ] Update README
- [ ] Add LICENSE file

---

## 📋 Phase 10: State Management & Error Handling (Week 5)

### State Management
- [ ] Implement proper UiState for all screens
- [ ] Handle Loading state with shimmer/skeleton
- [ ] Handle Success state
- [ ] Handle Error state with retry option
- [ ] Handle Empty state with helpful message

### Error Handling
- [ ] Network errors (timeout, no connection)
- [ ] Firebase errors (auth failed, permission denied)
- [ ] Upload errors (storage full, file too large)
- [ ] Location errors (permission denied, GPS off)
- [ ] Show user-friendly error messages
- [ ] Add retry mechanisms
- [ ] Log errors for debugging

### Edge Cases
- [ ] Handle GPS disabled
- [ ] Handle location permission denied
- [ ] Handle WhatsApp not installed
- [ ] Handle camera permission denied
- [ ] Handle no items in area
- [ ] Handle user profile incomplete
- [ ] Handle Firebase quota exceeded

---

## 📝 Git Commit Guidelines

**Format**: `type: description`

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code refactoring
- `style`: UI/styling changes
- `docs`: Documentation
- `test`: Testing
- `chore`: Build/config changes
