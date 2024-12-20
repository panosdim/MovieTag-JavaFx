# Movie Tag

A JavaFX application that write tags and cover art images in mp4 and mkv video files.
It uses the [JSON API](http://api.themoviedb.org/) provided by [TMDb](https://www.themoviedb.org/).

## Screenshots

![main](https://private-user-images.githubusercontent.com/10371312/388969086-02d7c28e-3183-4fe1-a3a7-c8e2738e286f.PNG?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MzIyODYyNDAsIm5iZiI6MTczMjI4NTk0MCwicGF0aCI6Ii8xMDM3MTMxMi8zODg5NjkwODYtMDJkN2MyOGUtMzE4My00ZmUxLWEzYTctYzhlMjczOGUyODZmLlBORz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDExMjIlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQxMTIyVDE0MzIyMFomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWUyZmFhYTRjYWQ2MDViMWNiYjk4ODg0ZjcxOTZjODQ1ODFhNDQ4MjYzYmExNmU0MGY1YjkyMmRjMWQ5MjcyNzImWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0In0.ufs_jj628P9vRDfoILt5HKa4otWbBZXAxLnFFSFFpB8)
![search](https://private-user-images.githubusercontent.com/10371312/388969079-f0a29425-0612-400c-93f5-31ccdfd548ca.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MzIyODYyNDAsIm5iZiI6MTczMjI4NTk0MCwicGF0aCI6Ii8xMDM3MTMxMi8zODg5NjkwNzktZjBhMjk0MjUtMDYxMi00MDBjLTkzZjUtMzFjY2RmZDU0OGNhLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDExMjIlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQxMTIyVDE0MzIyMFomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTczYjgwYWJkZmJiMWRmZDYyMzBjZGJlZjc2MWNhMWE2NzllYWQwNTY4OTBkYjVlY2NlYzU4YTkzOWU1MmFkN2EmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0In0._qdMOx5rGgZv6CRH6mwpbSPi03-fK62--iLiNNydxro)
![write](https://private-user-images.githubusercontent.com/10371312/388969084-0a72cff6-4ff3-4fb8-ab2e-e90d2b21cf8a.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MzIyODYyNDAsIm5iZiI6MTczMjI4NTk0MCwicGF0aCI6Ii8xMDM3MTMxMi8zODg5NjkwODQtMGE3MmNmZjYtNGZmMy00ZmI4LWFiMmUtZTkwZDJiMjFjZjhhLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDExMjIlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQxMTIyVDE0MzIyMFomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTQ5MDM1YzJlZmYzODFkMDcxNzdjMDQ5MGI1YmY4YmNjZjY4OGEwODE4OWRlZWY2N2ZkOWY1MTliZjhjYzYxOTkmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0In0.Djsgn_KwF9wKqYbrFkcSWrCRtdmto1-6s8U3LvOrtVA)

## Build

In order to build the project you need [IntelliJ IDEA](https://www.jetbrains.com/idea/).
Clone the repository and open the project with IntelliJ.

Then in `MovieTag -> src -> main -> resources` folder create a `tmdb.properties` file with the following content

```
KEY=YOUR_KEY_FOR_TMDB_API
```

and replace `YOUR_KEY_FOR_TMDB_API` with your key from [TMDb API](http://api.themoviedb.org/).

Then you can run the project or generate native bundle and JAR files.

## Libraries Used

-   [JCodec](http://jcodec.org/docs/working_with_mp4_metadata.html) used for editing metadata of mp4 files.
-   [themoviedbapi](https://github.com/c-eg/themoviedbapi) used to access the TMDb API.
