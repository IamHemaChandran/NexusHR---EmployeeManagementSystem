# Setting Up Real Email (SMTP)

## Step 1 — Download Jakarta Mail JAR
Go to: https://mvnrepository.com/artifact/com.sun.mail/jakarta.mail/2.0.1
Click "jar" to download → rename file to `jakarta.mail.jar`
Place it in your `EMS/lib/` folder.

## Step 2 — Get a Gmail App Password
1. Go to myaccount.google.com → Security
2. Enable 2-Step Verification
3. Search "App Passwords" → Create one named "EMS Pro"
4. Copy the 16-character password (e.g. abcd efgh ijkl mnop)

## Step 3 — Configure in EMS Pro
1. Run the app → Login → click **Settings** in the sidebar
2. Go to **Email / SMTP Settings**
3. Fill in:
   - SMTP Host: `smtp.gmail.com`
   - Port: `587`
   - Gmail Address: `yourname@gmail.com`
   - App Password: paste the 16-char password (no spaces)
   - Sender Name: `EMS Pro`
4. Click **Save & Test** — you'll get a test email

## Step 4 — Send emails
- Go to **Email / Chat** panel
- Pick a contact whose email is in the DB
- Click **✉ Send Email**
- Fill subject & body → click Send

## Login Credentials
| Username | Password  | Role     |
|----------|-----------|----------|
| admin    | admin123  | Admin    |
| admin1   | admin1234 | Admin    |
| jeel     | 123456789 | Admin    |
| hr1      | hr123456  | HR       |
| emp1     | emp123456 | Employee |
