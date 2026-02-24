# Email Configuration Guide

## Setting up Email for Your Application

The application now has email functionality enabled. To configure it properly, follow these steps:

## 1. Gmail SMTP Setup (Default Configuration)

If you're using Gmail as your email provider:

1. **Enable 2-Factor Authentication** on your Gmail account
2. **Generate an App Password**:
   - Go to Google Account Settings
   - Navigate to Security > 2-Step Verification > App passwords
   - Generate a new app password for "Mail"
3. **Set Environment Variables** (recommended for security):
   - `MAIL_USERNAME`: your Gmail address
   - `MAIL_PASSWORD`: the app password generated above

## 2. Alternative SMTP Providers

You can configure other email providers by changing the settings in `application.properties`:

- **Outlook/Hotmail**: host=`smtp-mail.outlook.com`, port=`587`
- **Yahoo**: host=`smtp.mail.yahoo.com`, port=`587`
- **Custom SMTP**: adjust host, port, and authentication settings as needed

## 3. Environment Variables

For security reasons, it's recommended to use environment variables:

```bash
# Linux/Mac
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password

# Windows Command Prompt
set MAIL_USERNAME=your-email@gmail.com
set MAIL_PASSWORD=your-app-password

# Windows PowerShell
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-app-password"
```

## 4. Features Enabled

With this configuration, the application now supports:
- User registration email verification
- Password reset emails
- Contact form notifications to administrators
- Other system notifications

## 5. Troubleshooting

If emails aren't sending:

1. Check that your SMTP settings are correct
2. Verify that your email provider allows SMTP access
3. Confirm that your credentials are correct
4. Check firewall settings if running on a server
5. Look at application logs for specific error messages