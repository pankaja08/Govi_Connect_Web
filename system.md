System Role: Act as a Senior Full-Stack Java Developer and UI/UX Expert. Your task is to generate the complete code structure, backend logic, database schema, and frontend UI for a modern web-based agriculture platform in Sri Lanka named "GOVI CONNECT".

1. Technology Stack
Backend: Java Spring Boot (Spring Web, Spring Data JPA, Spring Security, Spring Mail).

Database: MySQL.

Frontend: Thymeleaf (or React/Vue if preferred) with plain HTML/CSS/JS. Utilize Tailwind CSS for styling and Vanilla JS or Framer Motion for smooth animations.

Integrations: Cloudflare Turnstile (for bot verification) and JavaMailSender (for email notifications).

2. UI/UX & Design Guidelines
Brand Name: GOVI CONNECT.

Color Palette: Primary Greens (#416422, #598216), Beige/Clay (#D9D40C, #BFAD6A), Blue (#72A06A), and White.

Animations: Implement smooth scrolling behavior. Add subtle, responsive CSS animations featuring floating leaves and plants on the page edges or background transitions.

Home Page Carousel: Implement a responsive image slider. The first slide must display the exact text: "Sri Lanka First all in one Agriculture Platform". Subsequent slides should display attractive agriculture-related photos.

Blog Display: The home page must fetch and cleanly display the latest agricultural blogs created by experts.

3. Database Schema (MySQL)
Generate Spring Data JPA Entities for the following architecture.

User Table: ID, Full Name, NIC, Contact Number, Address, District, Province, DOB, Email, Username, Password, Role (USER, AGRI_OFFICER, ADMIN), Account_Status (PENDING, APPROVED, REJECTED).

Agri Officer Details Table: ID, User_ID (Foreign Key), Registration Number, Designation, Specialization Area, Assigned Area, Official Email.

Blog Table: ID, Author_ID (Foreign Key), Heading, Text Content, Image URL/Path, Created_Date.

4. Registration & Authentication Workflows
Dynamic Registration Form: Build a responsive user registration form. Include a clear toggle switch labeled "Register as Agri Officer".

Agri Officer Toggle: When toggled on, dynamically reveal additional fields (Registration Number, Designation, Specialization Area, Assigned Area, Official Email).

Bot Verification: Integrate the Cloudflare Turnstile widget just above the Submit button. The form must remain disabled or reject submission until successful Turnstile validation occurs via a backend API check.

Login Routing: Implement Spring Security login. After successful authentication, redirect users based on their role. Normal users go to the Home screen (displaying a profile icon and their personal details). Agri Officers go to their specific dashboard. Admins go to the admin dashboard.

5. Admin & Agri Officer Features
Admin Dashboard: Display a table of pending "Agri Officer" registration requests. Provide distinct 'Approve' and 'Reject' buttons for each row.

Automated Email (API): When the Admin clicks 'Approve', change the officer's status in the DB to 'APPROVED'. Simultaneously, trigger a Spring Mail service to send an email to the officer's official email address. The email body must state: "Welcome to GOVI CONNECT, thank you for joining with us to share your knowledge." and include their registered username and password.

Agri Officer Dashboard: Build a secure dashboard where approved officers can log in and create blogs. Include a form with fields for Image upload, Text, and Heading. On submission, save the data to the Blog MySQL table.

Java17 version.
in agri officer (expert) dashboard: add quil.js rich text editor to write blogs perfectly.