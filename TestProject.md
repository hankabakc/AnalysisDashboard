# Test Project for New Developer

## Overview
This test project is designed to assess your ability to work with a Spring Boot application, integrate with a PostgreSQL database, and display data in a web interface. You will implement a feature to list all Lines and PLCs (Programmable Logic Controllers) along with their statuses, and for each Line, show the barcodes that have been processed.

## Project Structure
The application is a Spring Boot project with the following key components:
- **Entities**: PlcInfo (and potentially others for Lines and Barcodes)
- **Repositories**: PlcInfoRepository (JPA repositories for data access)
- **Services**: PlcInfoService (business logic layer)
- **Controllers**: IndexController, ExampleController (handling web requests)
- **Templates**: index.html, example.html (Thymeleaf templates for rendering views)
- **Database**: PostgreSQL running in Docker, with data populated from SQL files in the `DB` folder

## Requirements
Implement the following functionality:

### 1. List All Lines and PLCs with Status
- Retrieve all Line information from the database (likely from `line_info` table).
- Retrieve all PLC information from the database (from `plc_info` table).
- Display each Line and PLC with its current status (e.g., active/inactive, online/offline, etc.).
- The status should be derived from the data in the database (check `line_log` and `plc_log` tables for status information).

### 2. Show Barcodes Processed per Line
- For each Line, query the `barcode_data` table to find barcodes associated with that line.
- Display the list of barcodes processed in each line.
- Include relevant details for each barcode (e.g., barcode value, processing timestamp, etc.).

## Implementation Guidelines
- Use Spring Data JPA repositories to query the database.
- Create or extend services to handle business logic for fetching and combining data.
- Modify or create new controllers to handle requests and pass data to views.
- Update Thymeleaf templates to render the data in a user-friendly format (e.g., tables or lists).
- Ensure proper error handling and data validation.
- Follow the existing code style and structure in the project.

## Expected Output
The web interface should display:
- A list or table of all Lines, showing their ID, name, and status.
- For each Line, a sub-list or expandable section showing the barcodes processed, with details like barcode ID, value, and processing date.

## Database Schema Notes
- `DDL.sql`: Contains the database schema (tables, constraints).
- `line_info.sql`: Sample data for lines.
- `plc_info.sql`: Sample data for PLCs.
- `line_log.sql`: Log data for line statuses.
- `plc_log.sql`: Log data for PLC statuses.
- `barcode_data.sql`: Data for barcodes processed per line.

## Testing
- Ensure the application starts without errors after following the setup steps in `HELP.md`.
- Verify that the data is displayed correctly by checking the populated database.
- Test with different scenarios (e.g., lines with no barcodes, multiple barcodes per line).

## Submission
Once implemented, the code should be committed and ready for review. Ensure all changes are documented and the application runs as expected.
