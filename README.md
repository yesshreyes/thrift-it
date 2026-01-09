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
- [ ] Fetch items from Firestore in `ItemRepository`
- [ ] Expose items from repository to ViewModel
- [ ] Update UI using StateFlow in Compose
- [ ] Handle empty state (no items)
- [ ] Handle loading state
- [ ] Handle Firestore errors

### Grid Layout Implementation
- [ ] Implement `LazyVerticalGrid` with 2 columns
- [ ] Display item cards with:
  - [ ] Image (using Coil)
  - [ ] Name
  - [ ] Price
  - [ ] Description
  - [ ] Item age
  - [ ] Distance from user
- [ ] Calculate distance using Haversine formula
- [ ] Format distance (e.g., "2.5 km away")

### Search Functionality
- [ ] Add search bar in top bar
- [ ] Implement search logic in repository
- [ ] Filter items by keywords
- [ ] Update UI with search results
- [ ] Handle empty search results
- [ ] Add clear search button

### Filter Functionality
- [ ] Create filter bottom sheet UI
- [ ] Add price range slider
- [ ] Add distance range slider
- [ ] Apply filters to Firestore query
- [ ] Update UI with filtered results
- [ ] Show active filters indicator
- [ ] Add clear filters option

### Item Detail & WhatsApp Integration
- [ ] Create item detail dialog/bottom sheet
- [ ] Show full item details on card click
- [ ] Add "Connect" button
- [ ] Get seller's phone from Firestore
- [ ] Create WhatsApp deep link with pre-filled message
- [ ] Launch WhatsApp with Intent
- [ ] Handle WhatsApp not installed case

---

## 📋 Phase 5: Core Features - Sell Screen 

### Image Picker
- [ ] Request camera permission
- [ ] Implement `ActivityResultContracts.PickVisualMedia()`
- [ ] Add option to pick from gallery
- [ ] Add option to take photo
- [ ] Show selected image preview
- [ ] Handle permission denied
- [ ] Compress image before upload

### Upload Form
- [ ] Create form fields:
  - [ ] Item name (TextField)
  - [ ] Price (TextField with number input)
  - [ ] Description (TextField multiline)
  - [ ] Item age/condition (Dropdown/Radio)
- [ ] Add form validation
- [ ] Show validation errors
- [ ] Disable submit until valid

### Firebase Upload
- [ ] Upload image to Firebase Storage
- [ ] Get download URL
- [ ] Create item document in Firestore with:
  - [ ] Image URL
  - [ ] User ID (seller)
  - [ ] Location (lat/lng)
  - [ ] Timestamp
  - [ ] All form data
- [ ] Show upload progress
- [ ] Handle upload errors

### Notifications
- [ ] Create notification channel
- [ ] Show success notification when uploaded
- [ ] Add notification permission request (Android 13+)
- [ ] Notification should open app on click

### Post-Upload
- [ ] Clear form after successful upload
- [ ] Show success message
- [ ] Navigate to Buy screen to see uploaded item
- [ ] Add option to upload another item

---

## 📋 Phase 6: Core Features - Settings & Notifications 

### Settings Screen
- [ ] Display user profile info
- [ ] Add edit profile option
- [ ] Add update location button
- [ ] Update location in Firestore and Room
- [ ] Add address management
- [ ] Show app version
- [ ] Add sign out button
- [ ] Confirm sign out with dialog
- [ ] Clear all data on sign out

### Notification Screen
- [ ] Display list of notifications
- [ ] Store notifications in Room 
- [ ] Show notification for: 
  - [ ] Item uploaded successfully
- [ ] Mark notifications as read
- [ ] Delete notifications
- [ ] Handle empty state

---

## 📋 Phase 7: Offline-First Implementation

### Offline Architecture
- [ ] Make Room the single source of truth
- [ ] Always read from Room, not Firestore directly
- [ ] Set Firestore offline persistence enabled
- [ ] Implement repository pattern properly:
  - [ ] Emit data from Room
  - [ ] Sync Firestore in background
  - [ ] Update Room on Firestore changes
- [ ] Clear Room cache on sign out
- [ ] Cache user data in Room

### Network State Handling
- [ ] Create `NetworkObserver` to monitor connectivity
- [ ] Show network status indicator in UI
- [ ] Queue uploads when offline
- [ ] Sync when back online
- [ ] Handle sync conflicts

### Offline Upload Queue
- [ ] Store pending uploads in Room
- [ ] Mark items as "pending_upload"
- [ ] Update status on success
- [ ] Show pending indicator in UI

### Pull to Refresh
- [ ] Add `SwipeRefresh` to Buy screen
- [ ] Trigger Firestore fetch on pull
- [ ] Update Room with new data
- [ ] Show refresh indicator

### Pagination & Sync
- [ ] Pagination logic for Buy and other screens

---

## 📋 Phase 8: State Management & Error Handling (Week 5)

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

## 📋 Phase 9: UI/UX Polish & Beautification (Week 6)

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
- [ ] Add "THRIFT IT" branding in top bar
- [ ] Use consistent typography
- [ ] Add brand personality to copy

### UI Improvements
- [ ] Polish item card design
- [ ] Improve spacing and padding
- [ ] Add subtle shadows and elevations
- [ ] Improve button styles
- [ ] Add icons where appropriate
- [ ] Ensure proper contrast ratios
- [ ] Test on different screen sizes

---

## 📋 Phase 10: Testing & Optimization 

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

### Performance Optimization
- [ ] Optimize image loading with Coil caching
- [ ] Optimize LazyGrid performance
- [ ] Optimize database queries
- [ ] Reduce Firestore reads
- [ ] Implement proper image compression
- [ ] Profile app with Android Profiler
- [ ] Fix memory leaks
- [ ] Reduce app size

### Code Quality
- [ ] Extract hardcoded strings to resources
- [ ] Use constants for magic numbers
- [ ] Refactor duplicate code
- [ ] Ensure proper package structure

### Documentation
- [ ] Update README with:
  - [ ] Project description
  - [ ] Features list
  - [ ] Setup instructions
  - [ ] Screenshots
  - [ ] Tech stack
- [ ] Add code documentation (KDoc)
- [ ] Create CONTRIBUTING.md
- [ ] Add LICENSE file

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
