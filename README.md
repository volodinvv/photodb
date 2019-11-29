# photodb

This is very simple application for organize photo archive.

There are some actions available:

1. scan: find all images and video file in the directory, read metadata, calculate MD5 (for first 10M bytes in file) and save all into SQLite database.

2. copy: copy all files which was saved in database. Destination directory is calculated as <year>/<month>_<day>_<comment>_<equipment>
