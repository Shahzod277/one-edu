# Dashboard Statistics API

Base URL: `https://{host}:8084/api/audit-stats`

Barcha dashboard endpoint'lari **Bearer Token** talab qiladi (faqat mavjud 4 ta org/org-client endpoint'lardan tashqari).

---

## 1. Summary — Umumiy ko'rsatkichlar

```
GET /dashboard/summary
```

**Response:**
```json
{
  "totalOrganizations": 45,
  "activeClientSystems": 120,
  "todayTotal": 1523,
  "todaySuccess": 1400,
  "todayError": 123,
  "todayUniqueUsers": 890,
  "allTimeTotal": 245000,
  "allTimeSuccess": 230000,
  "allTimeError": 15000,
  "allTimeUniqueUsers": 35000
}
```

**Frontend uchun:** Bosh sahifadagi 4 ta katta card (bugungi so'rovlar, success, error, unique users) + all-time raqamlar.

---

## 2. Monthly Trend — Oylik trend (grafik)

```
GET /dashboard/monthly-trend?months=6
```

| Param | Default | Tavsif |
|-------|---------|--------|
| `months` | 6 | Necha oylik ma'lumot |

**Response:**
```json
[
  {
    "month": "2025-11",
    "totalLogs": 38000,
    "successLogs": 35000,
    "errorLogs": 3000,
    "uniqueUsers": 5200
  },
  {
    "month": "2025-12",
    "totalLogs": 42000,
    "successLogs": 39500,
    "errorLogs": 2500,
    "uniqueUsers": 5800
  }
]
```

**Frontend uchun:** Line chart yoki bar chart — X o'qi oylar, Y o'qi success/error soni.

---

## 3. Monthly Comparison — Oy-oyga solishtirish

```
GET /dashboard/monthly-comparison
```

**Response:**
```json
{
  "currentMonth": "2026-04",
  "currentTotal": 42000,
  "currentSuccess": 39500,
  "currentError": 2500,
  "currentUniqueUsers": 5800,
  "previousMonth": "2026-03",
  "previousTotal": 38000,
  "previousSuccess": 35000,
  "previousError": 3000,
  "previousUniqueUsers": 5200,
  "totalGrowthPercent": 10.53,
  "successGrowthPercent": 12.86,
  "errorGrowthPercent": -16.67,
  "uniqueUsersGrowthPercent": 11.54
}
```

**Frontend uchun:** O'sish/pasayish ko'rsatkichlari — yashil yuqoriga o'q (ijobiy) yoki qizil pastga o'q (salbiy). Masalan: "So'rovlar +10.53%, Xatolar -16.67%".

---

## 4. Top Organizations — Eng faol tashkilotlar

```
GET /dashboard/top-organizations?limit=10
```

| Param | Default | Tavsif |
|-------|---------|--------|
| `limit` | 10 | Nechta org ko'rsatilsin |

**Response:**
```json
[
  {
    "organizationId": 1,
    "organizationCode": "TATU",
    "organizationName": "Toshkent axborot texnologiyalari universiteti",
    "totalLogs": 15000,
    "successLogs": 14200,
    "errorLogs": 800,
    "successRate": 94.67
  }
]
```

**Frontend uchun:** Rating jadvali — tashkilot nomi, jami so'rovlar, success rate (progress bar bilan).

---

## 5. Top Error Organizations — Eng ko'p xato bergan tashkilotlar

```
GET /dashboard/top-error-organizations?limit=10
```

Response formati `top-organizations` bilan bir xil. `successRate` bu yerda **error rate** ko'rsatadi.

**Frontend uchun:** Qizil rangli jadval — muammoli tizimlarni aniqlash uchun.

---

## 6. Unique Users Trend — Unikal foydalanuvchilar dinamikasi

```
GET /dashboard/unique-users-trend?months=6
```

**Response:**
```json
[
  { "month": "2025-11", "uniqueUsers": 5200 },
  { "month": "2025-12", "uniqueUsers": 5800 },
  { "month": "2026-01", "uniqueUsers": 6100 }
]
```

**Frontend uchun:** Area chart — foydalanuvchilar o'sish dinamikasi.

---

## 7. Peak Hours — Soatlar kesimida yuk

```
GET /dashboard/peak-hours
```

**Response:**
```json
[
  { "hour": 0, "totalLogs": 120, "successLogs": 110, "errorLogs": 10 },
  { "hour": 1, "totalLogs": 80, "successLogs": 75, "errorLogs": 5 },
  { "hour": 9, "totalLogs": 5200, "successLogs": 4900, "errorLogs": 300 },
  { "hour": 14, "totalLogs": 4800, "successLogs": 4500, "errorLogs": 300 }
]
```

`hour` — 0 dan 23 gacha (24 soat formati).

**Frontend uchun:** Bar chart — qaysi soatlarda eng ko'p so'rov keladi. Yuk balansini tushunish uchun.

---

## 8. Error Breakdown — Xato turlari

```
GET /dashboard/error-breakdown?limit=20
```

| Param | Default | Tavsif |
|-------|---------|--------|
| `limit` | 20 | Nechta xato turi ko'rsatilsin |

**Response:**
```json
[
  {
    "errorMessage": "HEMIS HTTP 401 body={\"error\":\"Unauthorized\"}",
    "count": 450,
    "percentage": 35.5
  },
  {
    "errorMessage": "Sizga ruxsat yo'q",
    "count": 200,
    "percentage": 15.8
  }
]
```

**Frontend uchun:** Pie chart yoki donut chart — qaysi xatolar eng ko'p. Jadval ko'rinishida ham ko'rsatish mumkin.

---

## 9. Client Uptime — Client system faoliyati

```
GET /dashboard/client-uptime
```

**Response:**
```json
[
  {
    "clientSystemId": 5,
    "systemName": "HEMIS Student",
    "organizationName": "TATU",
    "domen": "student.tatu.uz",
    "lastSuccessAt": "2026-04-27T14:30:00",
    "lastErrorAt": "2026-04-26T09:15:00",
    "totalLogs": 8500
  }
]
```

`lastSuccessAt` — oxirgi muvaffaqiyatli so'rov. `null` bo'lsa — hech qachon muvaffaqiyatli bo'lmagan.

**Frontend uchun:** Jadval — har bir client system uchun "oxirgi faoliyat". Agar `lastSuccessAt` 24 soatdan eski bo'lsa — qizil (offline), aks holda yashil (online).

---

## Mavjud eski endpoint'lar

Bu endpoint'lar autentifikatsiya **talab qilmaydi**:

| Endpoint | Tavsif |
|----------|--------|
| `GET /org/daily?orgId=1` | Kunlik statistika (org bo'yicha) |
| `GET /org/monthly?orgId=1` | Oylik statistika (org bo'yicha) |
| `GET /org-client/daily?orgId=1` | Kunlik (org + client bo'yicha) |
| `GET /org-client/monthly?orgId=1` | Oylik (org + client bo'yicha) |

`orgId` ixtiyoriy — berilmasa barcha tashkilotlar uchun qaytaradi.
