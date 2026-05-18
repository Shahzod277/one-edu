# PKCE API — Mobil ilova uchun

One-ID orqali talaba autentifikatsiyasi. Client secret kerak emas.

## Flow

```
Mobil ilova                    Backend                      One-ID
    |                             |                            |
    |  1. GET /authorize          |                            |
    |  (code_challenge)           |                            |
    |---------------------------->|                            |
    |                             |  302 redirect              |
    |                             |--------------------------->|
    |                             |                            |
    |            User login/parol kiritadi                     |
    |                             |                            |
    |                             |  callback (code)           |
    |                             |<---------------------------|
    |                             |                            |
    |                             |  One-ID dan token oladi    |
    |                             |  HEMIS dan token oladi     |
    |                             |  Natijani saqlaydi         |
    |                             |                            |
    |  redirect: deep_link?session_id=xxx                      |
    |<----------------------------|                            |
    |                             |                            |
    |  2. POST /token             |                            |
    |  (session_id, code_verifier)|                            |
    |---------------------------->|                            |
    |                             |                            |
    |  JSON: {token, refreshToken, api_url}                    |
    |<----------------------------|                            |
```

## 1-qadam: code_verifier va code_challenge generatsiya qilish

Mobil ilova ishga tushganda:

```
code_verifier = random string (43-128 belgi, [A-Z, a-z, 0-9, -, _, ., ~])
code_challenge = Base64URL(SHA256(code_verifier))
```

### Misol (pseudocode):

```kotlin
// Android (Kotlin)
val codeVerifier = generateRandomString(64)  // SecureRandom bilan
val bytes = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(Charsets.US_ASCII))
val codeChallenge = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
```

```swift
// iOS (Swift)
let codeVerifier = generateRandomString(length: 64)
let data = codeVerifier.data(using: .ascii)!
let hash = SHA256.hash(data: data)
let codeChallenge = Data(hash).base64URLEncodedString()
```

**MUHIM:** `code_verifier` ni local saqlang (SharedPreferences / UserDefaults). Keyingi qadamda kerak bo'ladi.

---

## 2-qadam: Autentifikatsiya boshlash

Browser (Chrome Custom Tab / SFSafariViewController) ochib, shu URL ga yo'naltiring:

```
GET https://hemis.uz/api/auth/pkce/authorize
    ?code_challenge={code_challenge}
    &redirect_uri={sizning_deep_link}
```

### Parametrlar:

| Parametr | Tavsif | Misol |
|---|---|---|
| `code_challenge` | SHA256 hash (1-qadamda yasagan) | `E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM` |
| `redirect_uri` | Ilovangiz deep link URL'i | `myapp://auth-callback` |

### Misol:

```
https://hemis.uz/api/auth/pkce/authorize?code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&redirect_uri=myapp://auth-callback
```

Bu URL foydalanuvchini One-ID login sahifasiga olib boradi.

---

## 3-qadam: Deep link'dan session_id olish

Foydalanuvchi One-ID da login qilgandan keyin, browser sizning deep link'ga redirect bo'ladi:

```
myapp://auth-callback?session_id=abc123xyz...
```

Ilovangizda deep link handler'da `session_id` ni oling.

### Android (AndroidManifest.xml):

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="myapp" android:host="auth-callback" />
</intent-filter>
```

### iOS (Info.plist):

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>myapp</string>
        </array>
    </dict>
</array>
```

---

## 4-qadam: Token olish

Deep link'dan `session_id` olgandan keyin, backend'ga POST so'rov yuboring:

```
POST https://hemis.uz/api/auth/pkce/token
Content-Type: application/json

{
    "sessionId": "abc123xyz...",
    "codeVerifier": "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
}
```

### Muvaffaqiyatli javob (200):

```json
{
    "code": 200,
    "message": "Muvaffaqiyatli",
    "success": true,
    "data": {
        "token": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
        "api_url": "https://student.samtuit.uz/rest/v1"
    }
}
```

### Xatolik javoblari:

**401 — code_verifier noto'g'ri:**
```json
{
    "code": 401,
    "message": "code_verifier noto'g'ri",
    "success": false
}
```

**400 — session topilmadi yoki muddati o'tgan (5 daqiqa):**
```json
{
    "code": 400,
    "message": "Session topilmadi yoki muddati tugagan",
    "success": false
}
```

**400 — HEMIS xato (talaba topilmadi va boshqalar):**
```json
{
    "code": 400,
    "message": "STAT: student/university topilmadi: pinfl=...",
    "success": false
}
```

---

## Tokenlardan foydalanish

Muvaffaqiyatli javobdagi `token` va `api_url` orqali HEMIS API ga murojaat qilasiz:

```
GET {api_url}/account/me
Authorization: Bearer {token}
```

`refreshToken` orqali token yangilash:
```
POST {api_url}/auth/refresh-token
Authorization: Bearer {refreshToken}
```

---

## Muhim eslatmalar

- `code_verifier` har safar YANGI generatsiya qiling (qayta ishlatmang)
- Session 5 daqiqada muddati tugaydi — foydalanuvchi 5 daqiqa ichida login qilishi kerak
- Token bir martalik — POST /token bir marta chaqiriladi, ikkinchi marta "session topilmadi" qaytadi
- `redirect_uri` da o'zingiz register qilgan deep link scheme ishlatiladi
