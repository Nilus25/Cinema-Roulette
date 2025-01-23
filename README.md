# IMDBSelector

## Description
IMDBSelector is an application for the management of IMDB movies.
It allows the user to create profiles which contain information of the multimedia that has seen from different watchlists that have to be imported from IMDB.
The user can view the movies they have and haven't seen from a concrete watchlist, while adding or deleting seen movies from their profiles.
These watchlist can be filtered by genre, rating, year, type(i.e. movie or TV show) or by keywords, like the title or the director contains X word.
The main attraction of this application is the selection of a random movie given the filters for the user to watch, also storing the last generated in case the user logs in after watching it, to be added to seen movies.
There are two modes for the selection, a full random one from the watchlist, or a not seen one, which will only select from the movies of the watchlist that the user hasn't seen yet.
## Motivation
The motivation behind this project was the need to have a tool that could help me decide what to watch when I was bored,
because my watchlist has become quite extensive and I spent more time deciding what to watch than actually watching something.
As I am currently on vacations and I have some free time I was in a hurry to make this tool, thus explaining why it isn't commented at all (even though I think the code is pretty self-explanatory).
I have some ideas for the future as would be to add different languages, specifically catalan and spanish. The option to not open URLs in case there is no internet connection.
To add a mode for the filters to be inclusive or exclusive (now is exclusive on the different parameters but inclusive internally on genres and types).
To add data extraction from Rotten Tomatoes, the images from IMDB and Justwatch information.
And finally some watchlist operations like the union and intersection of watchlist.


## Requirements
- Java 11 (Amazon Corretto 11 recommended)

## Installation
1. Just download the folder.

## Usage
Run the JAR file using the following command:
```sh
java -jar path/to/IMDBSelector.jar
```
Executable (Windows): Use the executable file.