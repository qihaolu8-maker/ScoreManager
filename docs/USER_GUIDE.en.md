# User guide / English

## Accounts and databases

Create an administrator when opening a new empty database. New passwords must contain at least eight characters. Existing databases use their own accounts. Administrators can add, edit and delete local accounts through the account-management dialog. Leave an edited password empty to retain the current password; passwords are not shown in the account list.

Legacy plaintext passwords migrate to salted PBKDF2 hashes after successful login. Back up before upgrading and keep using the new version after migration; older versions cannot verify the new hashes. Unmigrated accounts and old backups may still contain plaintext passwords. Account authentication is local application access control, not database-file encryption. Someone with filesystem access may still read student records. Keep database files in a trusted location.

## Classes and student points

Use the organization tree to manage majors and classes. Select a class to add students. Student IDs are unique within a database. Search and sort the table, add or subtract points with a reason, and review change history. Use the in-app Help and visible controls for editing, profiles, recycle-bin restoration and undo. Undo history belongs to the current run and is not a long-term backup.

## Import the 50-student example

1. Use a separate test environment so synthetic students are not mixed with a real roster.
2. Choose the CSV import action and select `examples/ScoreManager-50-students.csv`.
3. An empty database should import 50 students and skip zero: two majors, five classes, ten students per class.
4. Save, close normally and reopen to check persistence.
5. Importing the same file again should add zero students and skip 50 duplicate IDs.

CSV files use UTF-8. The recommended columns, in order, are major, class, student ID, student name and score. Use the provided header or example:

```csv
专业,班级,学号,姓名,总分数
Test major,Test class,TEST0001,Test student,0
```

Duplicate IDs, conflicting class ownership, missing required fields and invalid scores are skipped. Import adds records to the current database rather than replacing every student. Exported CSV can be opened in spreadsheet software; HTML reports can be opened and printed in a browser.

## Save and shortcuts

| Action | Windows / Linux | macOS |
| --- | --- | --- |
| Save | Ctrl+S | Command+S |
| Undo | Ctrl+Z | Command+Z |
| Quit | Close the main window | Close the main window or Command+Q |

Buttons provide the same save and undo actions. Normal exit waits for saving. If saving fails, address the error and try again instead of force-killing the app.

## Local files

| Platform | Default directory |
| --- | --- |
| Windows | `%USERPROFILE%\AppData\LocalLow\ScoreManager` |
| macOS | `~/Library/Application Support/ScoreManager` |
| Linux | An absolute `$XDG_DATA_HOME/ScoreManager`, otherwise `~/.local/share/ScoreManager` |

`settings.properties` remembers the last selected database. The default database is `ScoreData.db`. A `backups/` directory beside the current database holds startup backups. Files are outside the application bundle. On macOS, paste the directory into Finder's Command+Shift+G dialog.

Switching to another database requires signing in to that database again. Renaming or moving the current database through the app updates its remembered path. Do not move or concurrently edit it with other software while the app is open.

## Upgrade or migrate between systems

1. Save and close the old application normally.
2. Back up the actual `.db` file and any backups you want to retain.
3. Copy the database to a writable location on the destination computer and install the matching package.
4. Initialize the new environment, then use the database switch action to select the copied database.
5. Sign in with the copied database's accounts and verify the roster and scores.

Do not transfer the old `settings.properties`: absolute paths may not exist on the new computer. Reconnect any external drive needed for the remembered database. If the app reports that the previous database is unavailable, restore the file or storage device rather than deleting data.

## Uninstall and support

Close the application and remove its app directory or `.app` bundle. This does not automatically remove personal databases. Delete personal data separately only after deciding what to back up.

For [Issues](https://github.com/qihaolu8-maker/ScoreManager/issues), include the version, OS, chip, steps, expected result and actual error. Do not upload student records, complete databases, passwords or identifiable screenshots to the public repository. Use a minimal synthetic reproduction instead.
