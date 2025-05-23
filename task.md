# Rooster Enthusiast App – AI-Driven Task Checklist

To help you develop your **Rooster Enthusiast App** efficiently using AI agents like GitHub Copilot, Back4App’s MCP (Multi-Cloud Prototyping), and Firebase Studio, I’ll provide a set of clear, concise, and effective prompts tailored to your app’s requirements. These prompts are designed to leverage AI tools for generating code, UI, and backend logic, aligning with your vision for a culturally resonant platform with community engagement, fowl traceability, marketplace, and order features. The prompts focus on simplicity and efficiency, targeting key tasks for your MVP in Kotlin with Jetpack Compose, Back4App (Parse) as the primary backend, and Firebase for optional features (e.g., Crashlytics, Vertex AI). I’ll organize prompts by module (Auth, Community, Traceability, Marketplace) and include guidance on how to use them with specific AI tools. Each prompt is crafted to produce actionable code or assets, minimizing manual refinement.

### Guidelines for Using Prompts
- **GitHub Copilot**: Use these prompts as comments in Android Studio (e.g., `// Prompt text`). Copilot will suggest code based on the comment and context. Ensure your project is set up with the Parse SDK and Jetpack Compose dependencies (as per previous artifacts).
- **Back4App MCP**: Enter prompts in the Back4App dashboard’s MCP interface or associated AI tool (e.g., Cursor, if integrated). Specify “Kotlin” and “Jetpack Compose” for Android compatibility.
- **Firebase Studio**: Use prompts in Firebase Studio’s App Prototyping or App Testing agent interfaces. Specify “Android” and “Jetpack Compose” for UI tasks or “Kotlin” for backend logic.
- **Best Practices**:
  - Be specific in prompts (e.g., include class names, fields, or UI components).
  - Review generated code for errors, especially for Parse queries or Firebase integrations.
  - Test generated code in a small scope (e.g., one screen) before integrating.
  - Use prompts iteratively, refining outputs as needed (e.g., “Add error handling to the previous code”).

### Prompts by Module

#### 1. AuthModule (User Authentication)
**Objective**: Implement user registration, login, and role management (Farmer/Consumer) using ParseUser.

1. **Prompt for Login/Registration UI (GitHub Copilot, Firebase Studio)**:
   ```kotlin
   // Generate a Jetpack Compose screen for user login and registration with fields for username, password, and a radio button for role (Farmer or Consumer). Include buttons for Login and Register that call ParseUser methods. Show error messages if authentication fails.
   ```
   - **Tool**: Use in Android Studio (Copilot) or Firebase Studio’s App Prototyping agent.
   - **Expected Output**: A Composable function like `LoginScreen` with TextFields, RadioButtons, Buttons, and ParseUser calls (similar to `LoginActivity.kt` from previous artifacts).
   - **Usage**: Place in `LoginActivity.kt`. Ensure Parse SDK is initialized in `App.kt`.

2. **Prompt for ParseUser Role Management (GitHub Copilot, Back4App MCP)**:
   ```kotlin
   // Create a Kotlin function to set a user role (Farmer or Consumer) in ParseUser after registration. Store the role in a "role" field and set an ACL so only the user can modify their profile.
   ```
   - **Tool**: Use in Android Studio (Copilot) or Back4App MCP.
   - **Expected Output**: A function like:
     ```kotlin
     fun setUserRole(user: ParseUser, role: String) {
         user.put("role", role)
         val acl = ParseACL()
         acl.setPublicReadAccess(true)
         acl.setWriteAccess(user, true)
         user.setACL(acl)
         user.saveInBackground()
     }
     ```
   - **Usage**: Call after `signUpInBackground` in `LoginActivity.kt`.

3. **Prompt for Checking Current User (GitHub Copilot)**:
   ```kotlin
   // Write a Kotlin function to check if a ParseUser is logged in and navigate to MainActivity if true, otherwise stay on LoginActivity. Use Intent for navigation.
   ```
   - **Tool**: Android Studio (Copilot).
   - **Expected Output**: Code like:
     ```kotlin
     if (ParseUser.getCurrentUser() != null) {
         startActivity(Intent(this, MainActivity::class.java))
         finish()
     }
     ```
   - **Usage**: Add to `LoginActivity.kt`’s `onCreate`.

#### 2. CommunityModule (Social Feed)
**Objective**: Build a feed for users to share posts (text and images) with like/comment functionality.

1. **Prompt for Community Feed UI (GitHub Copilot, Firebase Studio, Back4App MCP)**:
   ```kotlin
   // Generate a Jetpack Compose screen for a community feed with a text input and button to post, and a LazyColumn to display posts. Each post is a Card with username, content, and a Like button. Fetch posts from a Parse "Post" class with fields: content (String), user (Pointer to _User), likes (Number).
   ```
   - **Tool**: Use in Android Studio (Copilot) or Firebase Studio/Back4App MCP.
   - **Expected Output**: A Composable function like `CommunityScreen` (similar to `MainActivity.kt` from previous artifacts).
   - **Usage**: Place in `MainActivity.kt`. Create a “Post” class in Back4App with `content`, `user`, and `likes` fields.

2. **Prompt for Posting to Back4App (GitHub Copilot, Back4App MCP)**:
   ```kotlin
   // Write a Kotlin coroutine function to save a post to a Parse "Post" class with fields: content (String), user (Pointer to _User), likes (Number, default 0). Use ParseObject.saveInBackground and refresh the feed after saving.
   ```
   - **Tool**: Android Studio (Copilot) or Back4App MCP.
   - **Expected Output**: Code like:
     ```kotlin
     suspend fun createPost(content: String) {
         val post = ParseObject("Post")
         post.put("content", content)
         post.put("user", ParseUser.getCurrentUser())
         post.put("likes", 0)
         post.saveInBackground()
         // Refresh feed logic
     }
     ```
   - **Usage**: Call in `CommunityScreen`’s Button `onClick`.

3. **Prompt for Like Functionality (GitHub Copilot)**:
   ```kotlin
   // Add a Like button to a Jetpack Compose post Card that increments the "likes" field of a Parse "Post" object. Update the UI to reflect the new like count.
   ```
   - **Tool**: Android Studio (Copilot).
   - **Expected Output**: Code like:
     ```kotlin
     Button(onClick = {
         post.increment("likes")
         post.saveInBackground()
     }) {
         Text("Like (${post.getInt("likes")})")
     }
     ```
   - **Usage**: Add to the post Card in `CommunityScreen`.

#### 3. TraceabilityModule (Fowl Profiles)
**Objective**: Allow farmers to add and view fowl profiles with name, type, and birth date.

1. **Prompt for Fowl Profile UI (GitHub Copilot, Firebase Studio, Back4App MCP)**:
   ```kotlin
   // Generate a Jetpack Compose screen for adding and viewing fowl profiles. Include a form with fields for name, type (Rooster/Hen via RadioButton), and birth date. Display a LazyColumn of fowl profiles as Cards with name, type, and birth date. Use Parse "Fowl" class with fields: name (String), type (String), birthDate (String), owner (Pointer to _User).
   ```
   - **Tool**: Use in Android Studio (Copilot) or Firebase Studio/Back4App MCP.
   - **Expected Output**: A Composable function like `FowlScreen` (similar to `FowlActivity.kt` from previous artifacts).
   - **Usage**: Place in `FowlActivity.kt`. Create a “Fowl” class in Back4App.

2. **Prompt for Adding Fowl to Back4App (GitHub Copilot, Back4App MCP)**:
   ```kotlin
   // Write a Kotlin coroutine function to save a fowl to a Parse "Fowl" class with fields: name (String), type (String), birthDate (String), owner (Pointer to _User). Set an ACL so only the owner can modify. Refresh the fowl list after saving.
   ```
   - **Tool**: Android Studio (Copilot) or Back4App MCP.
   - **Expected Output**: Code like:
     ```kotlin
     suspend fun addFowl(name: String, type: String, birthDate: String) {
         val fowl = ParseObject("Fowl")
         fowl.put("name", name)
         fowl.put("type", type)
         fowl.put("birthDate", birthDate)
         fowl.put("owner", ParseUser.getCurrentUser())
         val acl = ParseACL()
         acl.setPublicReadAccess(true)
         acl.setWriteAccess(ParseUser.getCurrentUser(), true)
         fowl.setACL(acl)
         fowl.saveInBackground()
         // Refresh fowl list
     }
     ```
   - **Usage**: Call in `FowlScreen`’s Button `onClick`.

3. **Prompt for Basic Lineage (GitHub Copilot)**:
   ```kotlin
   // Create a Kotlin function to query a Parse "Fowl" class for a fowl’s parent using a parentId (Pointer to Fowl) field. Return the parent’s name or null if no parent exists.
   ```
   - **Tool**: Android Studio (Copilot).
   - **Expected Output**: Code like:
     ```kotlin
     suspend fun getFowlParent(fowlId: String): String? {
         val query = ParseQuery.getQuery<ParseObject>("Fowl")
         val fowl = query.get(fowlId)
         val parent = fowl.getParseObject("parentId")
         return parent?.getString("name")
     }
     ```
   - **Usage**: Use in `FowlScreen` to display lineage.

#### 4. MarketplaceModule (Listings)
**Objective**: Enable farmers to create and browse fowl listings.

1. **Prompt for Marketplace Feed UI (GitHub Copilot, Firebase Studio, Back4App MCP)**:
   ```kotlin
   // Generate a Jetpack Compose screen for a marketplace feed with a form to create listings (title, price) and a LazyColumn to display listings as Cards with title, price, and seller username. Use Parse "Listing" class with fields: title (String), price (String), owner (Pointer to _User).
   ```
   - **Tool**: Use in Android Studio (Copilot) or Firebase Studio/Back4App MCP.
   - **Expected Output**: A Composable function like `MarketplaceScreen` (similar to `MarketplaceActivity.kt`).
   - **Usage**: Place in `MarketplaceActivity.kt`. Create a “Listing” class in Back4App.

2. **Prompt for Creating Listings via REST API (GitHub Copilot, Back4App MCP)**:
   ```kotlin
   // Write a Kotlin function using OkHttp to create a listing in Parse "Listing" class via REST API. Include fields: title (String), price (String), owner (Pointer to _User). Use BuildConfig.PARSE_APP_ID and BuildConfig.PARSE_REST_API_KEY for authentication.
   ```
   - **Tool**: Android Studio (Copilot) or Back4App MCP.
   - **Expected Output**: Code like:
     ```kotlin
     fun createListing(title: String, price: String, callback: (String?, Exception?) -> Unit) {
         val json = JSONObject().apply {
             put("title", title)
             put("price", price)
             put("owner", JSONObject().apply { put("objectId", ParseUser.getCurrentUser().objectId) })
         }
         val requestBody = json.toString().toRequestBody("application/json".toMediaType())
         val request = Request.Builder()
             .url("https://parseapi.back4app.com/classes/Listing")
             .addHeader("X-Parse-Application-Id", BuildConfig.PARSE_APP_ID)
             .addHeader("X-Parse-REST-API-Key", BuildConfig.PARSE_REST_API_KEY)
             .post(requestBody)
             .build()
         OkHttpClient().newCall(request).enqueue(object : Callback {
             override fun onFailure(call: Call, e: IOException) { callback(null, e) }
             override fun onResponse(call: Call, response: Response) { callback(response.body?.string(), null) }
         })
     }
     ```
   - **Usage**: Update `ParseRestClient.kt` and call from `MarketplaceScreen`.

3. **Prompt for Filtering Listings (GitHub Copilot)**:
   ```kotlin
   // Create a Kotlin function to query Parse "Listing" class with a filter for listings where price is below a given value. Return a list of titles and prices.
   ```
   - **Tool**: Android Studio (Copilot).
   - **Expected Output**: Code like:
     ```kotlin
     suspend fun filterListings(maxPrice: Double): List<Pair<String, String>> {
         val query = ParseQuery.getQuery<ParseObject>("Listing")
         query.whereLessThan("price", maxPrice.toString())
         return query.find().mapNotNull {
             val title = it.getString("title")
             val price = it.getString("price")
             if (title != null && price != null) Pair(title, price) else null
         }
     }
     ```
   - **Usage**: Add a filter TextField in `MarketplaceScreen`.

#### 5. Offline Support
1. **Prompt for Local Caching with Parse (GitHub Copilot, Back4App MCP)**:
   ```kotlin
   // Write a Kotlin function to pin a Parse "Fowl" object to Local Datastore for offline access and query it locally. Handle errors if the pin or query fails.
   ```
   - **Tool**: Android Studio (Copilot) or Back4App MCP.
   - **Expected Output**: Code like:
     ```kotlin
     suspend fun cacheFowl(fowl: ParseObject) {
         try {
             fowl.pinInBackground()
         } catch (e: Exception) {
             // Handle error
         }
     }
     suspend fun getCachedFowls(): List<ParseObject> {
         val query = ParseQuery.getQuery<ParseObject>("Fowl")
         return query.fromLocalDatastore().find()
     }
     ```
   - **Usage**: Call in `FowlActivity.kt` after saving or fetching fowls.

2. **Prompt for Room Integration (GitHub Copilot)**:
   ```kotlin
   // Generate a Room Entity and DAO for a Fowl with fields: id (String, PrimaryKey), name (String), type (String), birthDate (String), ownerId (String). Include methods to insert and query by ownerId.
   ```
   - **Tool**: Android Studio (Copilot).
   - **Expected Output**: Code like `FowlDao.kt` from previous artifacts.
   - **Usage**: Use in `CoreModule` for offline caching.

#### 6. Multilingual Support
1. **Prompt for String Resources (GitHub Copilot, Firebase Studio)**:
   ```kotlin
   // Generate Android string resources for English, Telugu, Tamil, Kannada, and Hindi. Include strings for app_name, login, register, share_post, add_fowl, create_listing. Use Noto Sans font for Indic scripts.
   ```
   - **Tool**: Android Studio (Copilot) or Firebase Studio.
   - **Expected Output**: XML files like `res/values/strings.xml` and `res/values-te/strings.xml` from previous artifacts.
   - **Usage**: Place in `res/values` directories. Ensure Noto Sans is used in Compose (default on Android).

2. **Prompt for In-App Language Switcher (GitHub Copilot)**:
   ```kotlin
   // Create a Jetpack Compose DropdownMenu to switch between English, Telugu, Tamil, Kannada, and Hindi. Update the app locale using AppCompatDelegate.setApplicationLocales.
   ```
   - **Tool**: Android Studio (Copilot).
   - **Expected Output**: Code like:
     ```kotlin
     DropdownMenu(
         expanded = expanded,
         onDismissRequest = { expanded = false }
     ) {
         listOf("en", "te", "ta", "kn", "hi").forEach { lang ->
             DropdownMenuItem(text = { Text(lang) }, onClick = {
                 AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
                 expanded = false
             })
         }
     }
     ```
   - **Usage**: Add to a settings screen or `MainActivity`.

#### 7. Visual Design
1. **Prompt for Themed UI (GitHub Copilot, Firebase Studio)**:
   ```kotlin
   // Generate a Jetpack Compose MaterialTheme with a color palette: primary green (#4CAF50), secondary saffron (#FF5722), background beige (#FAF3E0). Use Noto Sans font for text. Apply to a Card for a marketplace listing.
   ```
   - **Tool**: Android Studio (Copilot) or Firebase Studio.
   - **Expected Output**: Code like:
     ```kotlin
     MaterialTheme(
         colorScheme = ColorScheme(
             primary = Color(0xFF4CAF50),
             secondary = Color(0xFFFF5722),
             background = Color(0xFFFAF3E0)
         ),
         typography = Typography(
             bodyLarge = TextStyle(fontFamily = FontFamily(Font(R.font.noto_sans)))
         )
     ) {
         Card { /* Listing content */ }
     }
     ```
   - **Usage**: Apply in `MarketplaceScreen`.

2. **Prompt for Custom Icon (Back4App MCP, Firebase Studio)**:
   ```kotlin
   // Generate a vector drawable for a rooster silhouette icon to use as the app logo in Android. Save as res/drawable/rooster_icon.xml.
   ```
   - **Tool**: Back4App MCP or Firebase Studio.
   - **Expected Output**: A vector XML file for the icon.
   - **Usage**: Set as `android:icon` in `AndroidManifest.xml`.

#### 8. Testing and Debugging
1. **Prompt for App Testing (Firebase Studio)**:
   ```kotlin
   // Create a test script to simulate adding a fowl to the Parse "Fowl" class and verify it appears in the list. Check for error messages if the save fails.
   ```
   - **Tool**: Firebase Studio’s App Testing agent.
   - **Expected Output**: A test script that automates UI interactions and checks Parse data.
   - **Usage**: Run in Firebase Studio to validate `FowlActivity`.

2. **Prompt for Crashlytics Debugging (GitHub Copilot)**:
   ```kotlin
   // Add a test crash button in Jetpack Compose that throws a RuntimeException to verify Firebase Crashlytics integration. Include a Toast to confirm the crash was triggered.
   ```
   - **Tool**: Android Studio (Copilot).
   - **Expected Output**: Code like:
     ```kotlin
     Button(onClick = {
         Toast.makeText(context, "Triggering test crash", Toast.LENGTH_SHORT).show()
         throw RuntimeException("Test crash")
     }) {
         Text("Test Crash")
     }
     ```
   - **Usage**: Add to `MainActivity.kt` temporarily, then check Firebase Crashlytics dashboard.

---

### How to Use Prompts Efficiently
- **Batch Prompts**: For large tasks (e.g., generating an entire screen), break into smaller prompts (UI, logic, data) to ensure accuracy.
- **Iterate**: If the generated code has errors, refine the prompt with more context (e.g., “Add error handling” or “Use coroutineScope for async”).
- **Validate**: Test each generated component in isolation (e.g., run `FowlActivity` to verify fowl addition).
- **Back4App Setup**: Ensure “Post,” “Fowl,” and “Listing” classes are created in the Back4App dashboard with appropriate fields.
- **AI Tool Selection**:
  - Use Copilot for real-time coding in Android Studio.
  - Use Back4App MCP for Parse-specific tasks (e.g., schema generation, Cloud Code).
  - Use Firebase Studio for UI prototyping or testing, especially if integrating Firebase features.

---

### Example Workflow
1. **Setup**: Update `build.gradle.kts` and `App.kt` with Back4App credentials (use previous artifacts).
2. **Authentication**: Use the `LoginScreen` prompt to generate `LoginActivity.kt`. Test login/registration.
3. **Community Feed**: Use the `CommunityScreen` prompt to generate `MainActivity.kt`. Create a “Post” class in Back4App and test posting.
4. **Fowl Profiles**: Use the `FowlScreen` prompt for `FowlActivity.kt`. Create a “Fowl” class and test adding/viewing fowls.
5. **Marketplace**: Use the `MarketplaceScreen` prompt for `MarketplaceActivity.kt`. Create a “Listing” class and test listing creation.
6. **Offline**: Use the caching prompt to enable Parse Local Datastore or Room.
7. **Multilingual**: Generate string resources for all languages and test locale switching.

---

### Next Steps
- Run the app and verify each module (Auth, Community, Traceability, Marketplace).
- Add navigation (e.g., BottomNavigation) to switch between screens:
  ```kotlin
  // Prompt: Generate a Jetpack Compose BottomNavigation with items for Home (Community), Fowl, Marketplace, and Profile. Navigate to MainActivity, FowlActivity, MarketplaceActivity, and ProfileActivity.
  ```
- Implement image uploads for posts and listings using Back4App’s file storage.
- Add transfer verification for orders using a new Parse class.

If you need prompts for specific features (e.g., image uploads, push notifications, or Cloud Code) or want to focus on one module, please let me know! I can also provide wireframes or refine existing artifacts.
