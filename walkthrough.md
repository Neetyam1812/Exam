# Walkthrough: React-based Google Calendar Clone with Spring Boot Integration

The calendar page in the Spring Boot application (`calendar.html`) has been rewritten with a custom-engineered **React-based Google Calendar clone**. This change aligns the system with the exact July 2026 dark/light UI design system while preserving all backend Thymeleaf attributes and REST endpoints.

---

## Architectural & Interface Changes

### 1. Unified React App in calendar.html
*   **[MODIFY] [calendar.html](file:///C:/Users/Neetyam%20Patel/Downloads/Examdb1/exam/src/main/resources/templates/admin/calendar.html)**:
    *   Replaced FullCalendar v6 scripts and native Bootstrap modals with an in-browser compiled React 18 application.
    *   Links React, ReactDOM, and Babel from CDN to compile modern JSX code dynamically.
    *   Links Tailwind CSS CDN to parse utilities and style classes instantly.
    *   Replaces the entire workspace area (excluding the global Thymeleaf sidebar fragment) with a single mounting root: `#react-calendar-root`.

### 2. Spring Boot REST Integration
*   **Data Feeds**: Loads database events by querying `GET /admin/calendar/events` on mount, mapping them to the React component states.
*   **Adding/Updating Events**: Communicates with `POST /admin/calendar/event/add`. Since the Spring Boot controller creates new entries, **editing** is handled elegantly in the UI by first sending a delete query for the old event ID and then posting the updated event.
*   **Status Toggles**: Updates task completion status via `POST /admin/calendar/event/toggle-complete`.
*   **Deletions**: Removes items using `POST /admin/calendar/event/delete`.
*   **Context Syncing**: Uses Thymeleaf inline scripting to safely pass logged-in parameters (`adminName` and the list of `allAdmins`) into window globals so React forms are populated correctly.

### 3. AI Conflict Detector & Room Storage
*   Since the database entity lacks a dedicated `room` column, room identifiers (e.g. `Room 101`) are appended as a metadata tag (`[Room: Room 101]`) inside the `description` string.
*   The React app transparently parses this tag on load and strips it from user-facing text areas.
*   The conflict detection check validates overlapping dates, double-booked rooms, and back-to-back exam separations. It highlights warnings with a red dot prefix on grid pills, underlines the date numbers in the month view, and lists warnings in the collapsible auditor drawer.

### 4. Design System Compliance
*   **Strict Palette Mapping**: Dark/light mode theme configuration mapped to the specifications:
    *   Dark main bg: `#1a1a1a`
    *   Dark cell bg: `#1e1e1e` (hover: `#2a2a2a`)
    *   Grid borders: `0.5px solid #3c3c3c`
    *   Text: Primary `#e8eaed`, Secondary `#9aa0a6`, Other-month dates `#5f6368`
    *   Rath Yatra event: `#0d652d` bg, `#81c995` text (green pill — holiday style)
*   **Formatting Rules**:
    *   Month view headers (SUN MON...) are uppercase, sized `11px`, with `0.8px` letter spacing.
    *   Date numbers are `13px` weight 500.
    *   Mini calendar highlights Today in a filled blue circle.
    *   Banishment of box shadows to keep the UI flat and professional.
    *   Includes "Search for people" inputs and collapsible checkbox calendars.
*   **Week & Day Views**:
    *   Features an hourly vertical grid with a red horizontal time indicator line and circle dot matching the current local time.

---

## Verification Status

*   Running `.\mvnw.cmd compile` verifies that the Java project builds and compiles cleanly.
