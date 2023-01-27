package projekt1;

import java.util.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
/**
 *
 * @author emir.bolin
 */
public class Projekt1 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String player = getUserName();        
        boolean playAgain = true;     
        while (playAgain){
            playAgain = game(player);
        }
    }

    /**
     * This function implements the game including saving statistics
     * The board is always the same as of createBoard function
     * @param player
     * @return true if the player wants to play again
     */
    static boolean game(String player){
        // The array representing the board which is 6 * 6
        int maxCoordinate = 6;  
        String[][] board = createBoard(maxCoordinate);
        System.out.println("Hello "+player+"!\nIn this game there are "+countMonsters(board)+" monsters. Defeate them all!");
        int wins = 0;
        int loses = 0;
        int kills = 0;
        String stats = statFetcher(player);
        if (stats != null && stats.length() > 0){
            // The syntax in the file is wins,loses,kills
            wins =  Integer.parseInt(stats.split(",")[0]);
            loses = Integer.parseInt(stats.split(",")[1]);
            kills = Integer.parseInt(stats.split(",")[2]);
        }
        
        // Place the player on the board. The starting position is always 2,2
        int[] xyArray = move(2, 2, maxCoordinate, board);  
        
        while (countMonsters(board) > 0 ){
            printPosition(xyArray, board);
            if (isFight(board, xyArray[0], xyArray[1])){
                
                // Some cells on the board have monsters that the player needs to defeat
                // Player starts every fight with 100 HP
                List<Object> res = fight("Monster", player,
                        getMonsterHp(board, xyArray[0], xyArray[1]), 100, getMonsterPower(board, xyArray[0], xyArray[1]),
                        100, true);
                
                // Every fight ends with either loss,escape or win
                // List<Object> requires function get inorder to fetch an element in the list res
                if (!isAlive( (String) res.get(0), (int) res.get(1), player)){
                    System.out.println("Game over!\nThere are "+countMonsters(board)+" monsters left");
                    loses++;        
                    statSaver(player, getWLK(wins, loses, kills));
                    return askToPlayAgain();
                }
                else if (isEscape((String) res.get(2))){
                    // Escape to random cell
                    xyArray = escape((int) xyArray[0], (int) xyArray[1], maxCoordinate);    
                }
                else{
                    // Player won readjust board
                    kills++;
                    board = removeMonster(board, xyArray[0], xyArray[1]);   
                    if (countMonsters(board) == 0){
                        wins++;
                        break;
                    }   
                    // Move after fight
                    xyArray = move(xyArray[0], xyArray[1], maxCoordinate, board);   
                }
            }   
            else
                 // Move from cell without fight
                xyArray = move(xyArray[0], xyArray[1], maxCoordinate, board);  
        }
        System.out.println("Congratulations you have deafeted all the monsters!");
        System.out.println("In total you have "+kills+" kills. \nYou have won "+wins+" and lost "+loses+" games.");
        System.out.println(getAscii("Here is your trophy!"));
        
        // Save user statistics to the file called playername_stat.txt
        statSaver(player, getWLK(wins, loses, kills));
        return askToPlayAgain();
    }
    
    /** 
     *  This function asks if the player wants to play the game again
     * @return true if a user player wants to play again
     */
    static boolean askToPlayAgain(){
        Scanner scan = new Scanner(System.in);
        boolean validInput = false;
        while (!validInput){
            System.out.println("Do you want to play again? Answer yes or no");
            String playAgain = scan.nextLine();
            if (playAgain.equalsIgnoreCase("yes")){
                return true;
            }
            if (playAgain.equalsIgnoreCase("no")){
                return false;
            }
            System.out.println("Invalid input, please try again");
        }
        return validInput;
    }
    
    /**
     * This function handles the fight between monster and player 
     * It is not dependent on the board and atacker can be both monster or player
     * @param attacker - monster always starts as attacker
     * @param defender 
     * @param ahp - health-points of attacker
     * @param dhp - health-points of defender
     * @param aPower - attack-power of attacker
     * @param dPower - defence-power of defender
     * @param isComputerAttacker  - boolean showing if monster is attacking or not
     * @return a list with objects user, hp, mode (see userHpMode function)
     */
    static List<Object> fight(String attacker, String defender, int ahp, int dhp, int aPower, int dPower, boolean isComputerAttacker ){
        Scanner scan = new Scanner(System.in);
        
        // Randomizes power to attacker and defender
        Random rand = new Random();
        // atackpower can be up to aPower
        int a = rand.nextInt(aPower);
        // defencepower can be up to dPower
        int d = rand.nextInt(dPower);
        System.out.println(attacker+"s turn to attack!\n"+attacker+"s HP is "+ahp+"\n"+defender+"s HP is "+dhp);
        if (!isComputerAttacker){
            
            // Player can choose to escape or continue to fight
            System.out.println("Press Enter to attack or X to escape");
            String userInput = scan.nextLine();
            if (userInput.equalsIgnoreCase("x")){
                return userHpMode(attacker, ahp, "Escape");
            }
        }
        
        System.out.println(attacker+" attacks!");
        
        // Checks if attack is stronger than defence
        if (a > d){
            int damage = a - d;
            System.out.println(defender+" lost "+damage+" HP!");
            dhp = dhp - damage;
            if (dhp < 1){
                System.out.println(attacker+" deafeted "+defender);
                return userHpMode(attacker, ahp, "Win");
            }    
        }
        else{
            System.out.println("The attack missed!"); // defence was stronger
        }        
        // Change roles and call fight again
        return fight(defender, attacker, dhp, ahp, dPower, aPower, !isComputerAttacker);
    }
    
    /**
     * This function makes sure that we are always on the board. 
     * In case the new coordinates are out of bounds a recursive call to itself is done
     * @param xx new x coordinate
     * @param yy new y coordinate
     * @param maxCoordinate is a size of board i.e. 6x6
     * @param board is a matrix representing a game-board
     * @return array with new coordinates
     */
    static int[] move(int xx, int yy, int maxCoordinate, String [][] board){
        Scanner scan = new Scanner(System.in);
        System.out.println("Where do you want to go? N, NE, NW, S, SE, SW, E, W?");
        String userInput = scan.nextLine();
        userInput = userInput.toUpperCase();
        int[] xyArray = moveToDirection(xx, yy, userInput);
        
        // Check if unchanged coordinates
        if(xyArray[0] == xx && xyArray[1] == yy){   
            System.out.println("Incorrect input enter one of the directions");
            // New recursive try to move
            return move(xx,yy, maxCoordinate, board);  
        }
        
        if (!isOnTheBoard(xyArray, maxCoordinate)){
            System.out.println("You cannot go "+userInput+", choose another direction");
            // New recursive try to move
            return move(xx,yy, maxCoordinate, board);  
        }
        return xyArray;
    }
    
    /**
     * This function creates a string for statistics file
     * @param wins
     * @param loses
     * @param kills
     * @return String
     */
    static String getWLK(int wins, int loses, int kills){
        return Integer.toString(wins)+","+Integer.toString(loses)+","+Integer.toString(kills);
    }
    /**
     * This function gathers 3 different objects in one list
     * @param user
     * @param hp
     * @param mode
     * @return a list with different types of objects
     */
    static List<Object> userHpMode(String user, int hp, String mode){
        return Arrays.asList(user, hp, mode);
    }
    
    /**
     * This function puts x and y coordinates in an array
     * @param x
     * @param y
     * @return array with coordinates
     */
    static int[] getCoordinates(int x, int y){
        int[] xy = new int[2];
        xy[0] = x;
        xy[1] = y;
        return xy;
    }
    
    /**
     * This function checks if the player is alive
     * @param winner
     * @param hp
     * @param player
     * @return true if player still has health-points
     */
    static boolean isAlive(String winner, int hp, String player){
        return winner.equalsIgnoreCase(player) && hp > 0;
    }
    
    /**
     * The function checks if the player can escape from a fight
     * @param mode
     * @return true if mode equals escape
     */
    static boolean isEscape(String mode){
        return mode.equalsIgnoreCase("escape");
    }
    
    /**
     * This function checks if the player is on the board
     * @param res is a array of x and y coordinates
     * @param maxCoordinate 
     * @return true if the player is on the board
     */
    static boolean isOnTheBoard (int[] res, int maxCoordinate){
        return (int) res[0] >= 0 && (int) res[0] < maxCoordinate && (int) res[1] >= 0 && (int) res[1] < maxCoordinate;
    }
    
    /**
     * This function randomizes a new cell to escape to
     * @param xx
     * @param yy
     * @param maxCoordinate
     * @return new coordinates in array
     */
    static int[] escape(int xx, int yy, int maxCoordinate){
        String[] directions = {"N","E","S","W","NE","NW","SE","SW"};
        Random rand = new Random();
        // Random number as index in directions
        int d = rand.nextInt(directions.length);    
        int[] xyArray = moveToDirection(xx, yy, directions[d]);
        if (!isOnTheBoard(xyArray, maxCoordinate)){ 
            // new recursive try to escape
            return escape(xx, yy, maxCoordinate);   
        }
        // new cordinates are valid
        return xyArray;  
    }
    
    /**
     * This function translates input i.e North to a new coordinates. 
     * Validation of cells in the board is done by move/escape after calling this function
     * @param xx
     * @param yy
     * @param direction
     * @return array with new coordinates 
     */
    static int[] moveToDirection (int xx, int yy, String direction){
        // Save input parameters in case the direction is invalid
        int x = xx;
        int y= yy;  
        
        switch (direction) {
            case "N" -> y--;
            case "E" -> x++;
            case "S" -> y++;
            case "W" -> x--;
            case "NE" -> {
                y--;
                x++;
            }
            case "NW" -> {
                y--;
                x--;
            }
            case "SE" -> {
                y++;
                x++;
            }
            case "SW" -> {
                y++;
                x--;
            }
            default -> {
            }
        }
        return getCoordinates(x, y);
    }
    
    /**
     * This functions includes meta data about the board
     * There are six monsters among the cells
     * @param maxCoordinate
     * @return an array of arrays
     */
    static String[][] createBoard(int maxCoordinate){
        String[][] board = new String[maxCoordinate][maxCoordinate];
        
        //             Monster,hp,power,place is the syntax for monster     
        board[0][0] = "Monster,55,50,You have come to the sea. Suddenly an octopus shows up! Fight him to death!";
        board[0][1] = "You have come to a waterfall";
        board[0][2] = "You have come to a mountain";
        board[0][3] = "You have come to a wall";
        board[0][4] = "You have come to a castle";
        board[0][5] = "Monster,60,70,You have come to a cave. Suddenly there comes a giant! Fight him to death!";
        
        board[1][0] = "You have come to a beach";
        board[1][1] = "You have come to the magic tree";
        board[1][2] = "You have come to a garden";
        board[1][3] = "You have come to a meadow full of flowers";
        board[1][4] = "You have come to a mountain";
        board[1][5] = "You have come to a wall";
        
        board[2][0] = "You have come to the coast.";
        board[2][1] = "You have come to a village";    
        board[2][2] = "You are at the city of Kuluskus";
        board[2][3] = "You have come to a tree house";
        board[2][4] = "You come to a cottage";
        board[2][5] = "You have come to a mountain";
        
        board[3][0] = "You have come to a barn";
        board[3][1] = "You have found some camels";
        board[3][2] = "You have come to a forest";
        board[3][3] = "You have come to a forest";
        board[3][4] = "Monster,50,40,You have come to a swamp. Suddenly a spider attacks you! Fight him to death!";
        board[3][5] = "You have come to a cliff";
        
        board[4][0] = "You have come to a desert";
        board[4][1] = "You have come to a sandy place";
        board[4][2] = "Monster,45,30,You come to a dark forest. Suddenly an ghost attacks you! Fight him to death!";
        board[4][3] = "You have realized that a someone is following you";
        board[4][4] = "You have come to a cliff";
        board[4][5] = "You have come to a volcano";
        
        board[5][0] = "Monster,50,50,You have come to a pyramid. Suddenly an a mummy attacks you! Fight him to death!";
        board[5][1] = "You have come to a lost city";
        board[5][2] = "You have come to a graveyard";
        board[5][3] = "You have come to a cliff";
        board[5][4] = "You have come to a volcano";
        board[5][5] = "Monster,80,100,You have entered hell. Suddenly the devil attacks you! Fight him to death!";
        
        return board;
    }
    
    /**
     * This function creates a username for the player
     * @return a string 
     */
    static String getUserName(){
        Scanner scan = new Scanner(System.in);
        System.out.print("Please write your username: ");
        String userInput = scan.nextLine();
        while (userInput.length() < 1)
            return getUserName();
        return userInput;
    }
    
    /**
     * This function checks if the cell has a monster on it
     * @param board
     * @param x
     * @param y
     * @return true if the cell has a monster on it
     */
    static boolean isFight(String[][] board, int x, int y){
        // Position with fight includes 4 cells
        return board[x][y].split(",").length == 4;    
    }
    
    /**
     * This function prints coordinates including ascii art presentation of the cell
     * @param xyArray
     * @param board
     */
    static void printPosition(int[] xyArray, String[][] board){
        int x = xyArray[0];
        int y = xyArray[1];
        if (isFight(board, x, y)){
            System.out.println(getAscii(board[x][y].split(",")[3]));
        }
        else{
            System.out.println(getAscii(board[x][y]));
        }
        // For debugging purposes
        //System.out.println(x+", "+y);    
    }
        
    /**
     * This function counts remaining monsters on the board
     * @param board
     * @return int
     */
    static int countMonsters (String[][] board){
        int m = 0;
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board.length; y++) {
                if (isFight(board, x, y)){
                    m++;
                }
            }
        }
        return m;
    }
    
    /**
     * This function removes defeated monsters from the board
     * Next time the player comes to the same cell the monster is not there
     * Only some parts of the old text is shown (first sentence of old text)
     * @param board
     * @param x
     * @param y
     * @return updated version of the board
     */
    static String[][] removeMonster(String[][] board, int x, int y){
        if (isFight(board, x, y)){
            board[x][y] = board[x][y].split(",")[3];
            // regular expression requires this syntax when searching for dot
            board[x][y] = board[x][y].split("\\.")[0];  
        }
        return board;
    }
    
    /**
     * This function checks how much health-points the monster have
     * @param board
     * @param x
     * @param y
     * @return int with monsters HP
     */
    static int getMonsterHp (String[][] board, int x, int y){
        return Integer.parseInt(board[x][y].split(",")[1]);
    }
    
    /**
     * This function checks how much power the monster have
     * @param board
     * @param x
     * @param y
     * @return int with monsters power
     */
    static int getMonsterPower (String[][] board, int x, int y){
        return Integer.parseInt(board[x][y].split(",")[2]);
    }
    
    /**
     * This function fetches the statistics from the file if it exists
     * Every player has an own file including wins,loses,kills
     * @param fileName
     * @return string (commaseparated)
     */
    static String statFetcher(String fileName) {        
        String line = null;
     
        try {         
            // Create a new file object
            File file = new File(fileName+"_stats.txt");            
                
            // Create a new FileReader object
            FileReader fileReader = new FileReader(file);
            
            // Create a new BufferedReader object
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            
            // Read the whole file and save it as a string
            StringBuilder content = new StringBuilder();
            
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line);
                content.append(System.lineSeparator());
                break; // just one line per player
            }            
            
            // Close the buffered reader
            bufferedReader.close();
        }
        catch (FileNotFoundException e) {
            return "";
        }
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return "";
        }
        return line;
    }

    /**
     * This function saves statistics in a file in the current directory
     * @param fileName
     * @param content is comma-separated string containing wins,loses,kills
     * @return void
     */
    static void statSaver(String fileName, String content) {     
        // Create a new file object
        File file = new File(fileName+"_stats.txt");
        
        // Create the file if it doesn't already exist
        try {
            file.createNewFile();
        }
        catch (FileNotFoundException e) {            
            System.out.println("Could not find file to save stats.");
            e.printStackTrace();          
        }
        catch (IOException e) {
            System.out.println("Could not save stats.");
            e.printStackTrace();
        }
        
        // Write statistics to the file
        Path path = Paths.get(file.getAbsolutePath());
        try {
            Files.write(path, content.getBytes());
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file.");
            e.printStackTrace();
        }
    }

    /**
     * This function takes in codeword and returns corresponding ascii-art
     * @param item
     * @return string
     */
    static String getAscii(String item){
        String res ="";
        if (item.contains("mountain")){
            res = """
                     _______________________
                    |                  - (  |
                   ,'-.                 . `-|
                  (____".       ,-.    '   ||
                    |          /\\,-\\   ,-.  |
                    |      ,-./     \\ /'.-\\ |
                    |     /-.,\\      /     \\|
                    |    /     \\    ,-.     \\
                    |___/_______\\__/___\\_____\\ """;
        }
        else if (item.contains("cottage")){
            res = """
                                                     /\\
                                                /\\  //\\\\
                                         /\\    //\\\\///\\\\\\        /\\
                                        //\\\\  ///\\////\\\\\\\\  /\\  //\\\\
                           /\\          /  ^ \\/^ ^/^  ^  ^ \\/^ \\/  ^ \\
                          / ^\\    /\\  / ^   /  ^/ ^ ^ ^   ^\\ ^/  ^^  \\
                         /^   \\  / ^\\/ ^ ^   ^ / ^  ^    ^  \\/ ^   ^  \\       *
                        /  ^ ^ \\/^  ^\\ ^ ^ ^   ^  ^   ^   ____  ^   ^  \\     /|\\
                       / ^ ^  ^ \\ ^  _\\___________________|  |_____^ ^  \\   /||o\\
                      / ^^  ^ ^ ^\\  /______________________________\\ ^ ^ \\ /|o|||\\
                     /  ^  ^^ ^ ^  /________________________________\\  ^  /|||||o|\\
                    /^ ^  ^ ^^  ^    ||___|___||||||||||||___|__|||      /||o||||||\\       |
                   / ^   ^   ^    ^  ||___|___||||||||||||___|__|||          | |           |
                  / ^ ^ ^  ^  ^  ^   ||||||||||||||||||||||||||||||oooooooooo| |ooooooo  |
                  ooooooooooooooooooooooooooooooooooooooooooooooooooooooooo""";
        }
        else if (item.contains("beach")){
            res = """
                             |
                           \\ _ /
                         -= (_) =-
                           /   \\         _\\/_
                             |           //o\\  _\\/_
                      _____ _ __ __ ____ _ | __/o\\\\ _
                    =-=-_-__=_-= _=_=-=_,-'|"'""-|-,_
                     =- _=-=- -_=-=_,-"          |
                       =- =- -=.--"\t""";
        }
        else if (item.contains("trophy")){
            res = """
                               ___________
                              '._==_==_=_.'
                              .-\\:      /-.
                             | (|:.     |) |
                              '-|:.     |-'
                                \\::.    /
                                 '::. .'
                                   ) (
                                 _.' '._
                                `\"\"\"\"\"""`""";  
        }
        else if (item.contains("camels")){
            res = """
                                                               //^\\\\
                                                           //^\\\\ #
                     q_/\\/\\   q_/\\/\\    q_/\\/\\   q_/\\/\\      #   #
                       ||||`    /|/|`     <\\<\\`    |\\|\\`     #   #
                  %*%*%**%**%**%*%*%**%**%**%****%*%**%**%**%*%*%**%**%**%""";
        }
        else if (item.contains("sand")){
            res ="""
                            ,,                               .-.
                           || |                               ) )
                           || |   ,                          '-'
                           || |  | |
                           || '--' |
                     ,,    || .----'
                    || |   || |
                    |  '---'| |
                    '------.| |                                  _____
                    ((_))  || |      (  _                       / /|\\ \\
                    (o o)  || |      ))("),                    | | | | |
                 ____\\_/___||_|_____((__^_))____________________\\_\\|/_/__""";
        }
        else if (item.contains("desert")){
            res = """
                             ,                        '           .        '        ,  
                     .            .        '       .         ,         
                                                                     .       '     +
                         +          .-'''''-.     
                                  .'         `.   +     .     ________||
                         ___     :             :     |       /        ||  .     '___
                    ____/   \\   :               :   ||.    _/      || ||\\_______/   \\
                   /         \\  :      _/|      :   `|| __/      ,.|| ||             \\
                  /  ,   '  . \\  :   =/_/      :     |'_______     || ||  ||   .      \\
                      |        \\__`._/ |     .'   ___|        \\__   \\\\||  ||...    ,   \\
                     l|,   '   (   /  ,|...-'        \\   '   ,     __\\||_//___          
                   ___|____     \\_/^\\/||__    ,    .  ,__             ||//    \\    .  ,
                             _/~  `""~`"` \\_           ''(       ....,||/       '   
                   ..,...  __/  -'/  `-._ `\\_\\__        | \\           ||  _______   .
                            /   '`  `\\   \\  \\-.\\        /(_1_,..      || /
                                                              ______/\'\''''""";
        }
        else if (item.contains("pyramid")){
            res ="""
                           ,/`.
                         ,'/ __`.
                       ,'_/_  _ _`.
                     ,'__/_ ___ _  `.
                   ,'_  /___ __ _ __ `.
                  '-.._/___...-"-.-..__`. """;
        }
        else if (item.contains("coast")){
            res = """
                                                          |
                                                        \\ _ /
                                                      -= (_) =-
                     .\\/.                               /   \\
                  .\\\\//o\\\\                      ,\\/.      |              ,~
                  //o\\\\|,\\/.   ,.,.,   ,\\/.  ,\\//o\\\\                     |\\
                    |  |//o\\  /###/#\\  //o\\  /o\\\\|                      /| \\
                  ^^|^^|^~|^^^|' '|:|^^^|^^^^^|^^|^^^\"\"\"\"\"\"""("~~~~~~~~/_|__\\~~~~~~~~~~
                   .|'' . |  '''""'"''. |`===`|''  '"" "" " (" ~~~~ ~ ~======~~  ~~ ~
                      ^^   ^^^ ^ ^^^ ^^^^ ^^^ ^^ ^^ "" \"""( " ~~~~~~ ~~~~~  ~~~ ~""";
        }
        else if (item.contains("village")){
            res = """
                  ~         ~~          __
                         _T      .,,.    ~--~ ^^
                   ^^   // \\                    ~
                        ][O]    ^^      ,-~ ~
                     /''-I_I         _II____
                  __/_  /   \\ ______/ ''   /'\\_,__
                    | II--'''' \\,--:--..,_/,.-{ },
                  ; '/__\\,.--';|   |[] .-.| O{ _ }
                  :' |  | []  -|   ''--:.;[,.'\\,/
                  '  |[]|,.--'' '',   ''-,.    |
                    ..    ..-''    ;       ''. '""";
        }
        else if (item.contains("castle")){
            res = """
                                                    |>>>
                                                    |
                                      |>>>      _  _|_  _         |>>>
                                      |        |;| |;| |;|        |
                                  _  _|_  _    \\\\.    .  /    _  _|_  _
                                 |;|_|;|_|;|    \\\\:. ,  /    |;|_|;|_|;|
                                 \\\\..      /    ||;   . |    \\\\.    .  /
                                  \\\\.  ,  /     ||:  .  |     \\\\:  .  /
                                   ||:   |_   _ ||_ . _ | _   _||:   |
                                   ||:  .|||_|;|_|;|_|;|_|;|_|;||:.  |
                                   ||:   ||.    .     .      . ||:  .|
                                   ||: . || .     . .   .  ,   ||:   |       \\,/
                                   ||:   ||:  ,  _______   .   ||: , |            /`\\
                                   ||:   || .   /+++++++\\    . ||:   |
                                   ||:   ||.    |+++++++| .    ||: . |
                                __ ||: . ||: ,  |+++++++|.  . _||_   |
                       ____--`~    '--~~__|.    |+++++__|----~    ~`---,              ___
                  -~--~                   ~---__|,--~'                  ~~----_____-~'   `~----~~""";
        }
        else if (item.contains("wall")){
            res = """
                                                                  |>>>
                                                                  |
                                                              _  _|_  _
                                                             |;|_|;|_|;|
                                                             \\\\.    .  /
                                                              \\\\:  .  /
                                                               ||:   |
                                                               ||:.  |
                                                               ||:  .|
                                                               ||:   |       \\,/
                                                               ||: , |            /`\\
                                                               ||:   |
                                                               ||: . |
                                __                            _||_   |
                       ____--`~    '--~~__            __ ----~    ~`---,              ___
                  -~--~                   ~---__ ,--~'                  ~~----_____-~'   `~----~~""";
        }
        else if (item.contains("garden")){
            res = """
                                                {} {}
                                          !  !  II II  !  !                         
                                       !  I__I__II II__I__I  !                      
                                       I_/|__|__|| ||__|__|\\_I                      
                                    ! /|_/|  |  || ||  |  |\\_|\\ !                   
                        .--.        I//|  |  |  || ||  |  |  |\\\\I        .--.       
                       /-   \\    ! /|/ |  |  |  || ||  |  |  | \\|\\ !    /=   \\      
                       \\=__ /    I//|  |  |  |  || ||  |  |  |  |\\\\I    \\-__ /      
                        }  {  ! /|/ |  |  |  |  || ||  |  |  |  | \\|\\ !  }  {       
                       {____} I//|  |  |  |  |  || ||  |  |  |  |  |\\\\I {____}      
                   __!__|= |=/|/ |  |  |  |  |  || ||  |  |  |  |  |  | \\|\\=|  |__!_
                   _I___|  ||/|__|__|__|__|__|__|| ||__|__|__|__|__|__|\\||- |__I__I_
                   -|---|- ||-|--|--|--|--|--|--|| ||--|--|--|--|--|--|-||= |--|--|-
                    |   |  || |  |  |  |  |  |  || ||  |  |  |  |  |  | ||  |  |  | 
                    |   |= || |  |  |  |  |  |  || ||  |  |  |  |  |  | ||= /\\^/\\ | 
                    |   |- || |  |  |  |  |  |  || ||  |  |  |  |  |  | ||= \\_\\_/ | 
                    |   |- || |  |  |  |  |  |  || ||  |  |  |  |  |  | ||- _// |_| 
                   _|___|  ||_|__|__|__|__|__|__|| ||__|__|__|__|__|__|_||  |\\\\/_/| 
                   -|---|- ||-|--|--|--|--|--|--|| ||--|--|--|--|--|--|-||= |//-|-| 
                        |- || |  |  |  |  |  |  || ||  |  |  |  |  |  | ||==//  | | 
                   ~~~~~~~~~^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^~~~\\\\~~~~~~""";
        }
        else if (item.contains("meadow")){
            res = """
                                           '    .''.                                                         
                                            '..'     '. \\/                                                  
                                                                 '.()o                                  
                                      _                                                           
                                    _(_)_                          wWWWw   _                      
                        @@@@       (_)@(_)   vVVVv     _     @@@@  (___) _(_)_     vVVVv     _    
                       @@()@@ wWWWw  (_)\\    (___)   _(_)_  @@()@@   Y  (_)@(_)    (___)   _(_)_  
                        @@@@  (___)     `|/    Y    (_)@(_)  @@@@   \\|/   (_)\\       Y    (_)@(_) 
                         /      Y       \\|    \\|/    /(_)    \\|      |/      |      \\|/    /(_)   
                      \\ |     \\ |/       | / \\ | /  \\|/       |/    \\|      \\|/    \\ | /  \\|/     
                      \\\\|//   \\\\|///  \\\\\\|//\\\\\\|/// \\|///  \\\\\\|//  \\\\|//  \\\\\\|//  \\\\\\|/// \\|///   
                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^  """;
        }
        else if (item.contains("lost city")){
            res = """
                         _.-""}
                        / "" ;
                    .-"` __] ',               ___
                    I_ ""__.`-,;             |   |
                      I_.,-"ii"{             !___!
                      | ||  ||  |        ,    | |
                      | ||  ||  |       .;    | |
                      | ||  ||  |       | \\   | |
                      | ||  ||  |       |  |  | |
                      | ||  ||  |       |  |  | |   __
                      | ||  ||  |       |  |  | |  |  |
                      | ||  ||  |   ;|  |  |  | |  |  |
                      | ||  ||  |"\\_/`,_|  |  | |  |  |  ___.--""`\\
                      | ||  ||  |       |  |\\.| |=,|  |""          `,
                      | ||  ||  |       |  |  | |  |  |____________.-+.__
                     _:_!|_,'!__!       |  |  | |_,!  !         __,I   `"|
                    :     |      `-""`,.!__!-,!_!_ '--'`,_,--\"""         |
                    |     ;___          `"-.-'    `,_.-'"            _..-'
                     `-._ |   \"""--,,_     |`""-.--'|         __.--""
                         `"--..__     ""--.|    |   |_,_  _.-'
                                 ""--.._   `-,__!_.-' _,""      
                                        ""--,____.--''""";
        }
        else if (item.contains("volcano")){
            res = """
                                (   (( . : (    .)   ) :  )                              
                                  (   ( :  .  :    :  )  ))                              
                                   ( ( ( (  .  :  . . ) )                                
                                    ( ( : :  :  )   )  )                                 
                                     ( :(   .   .  ) .'                                  
                                      '. :(   :    )                                     
                                        (   :  . )  )                                    
                                         ')   :   #@##                                   
                                        #',### " #@  #@
                                       #/ @'#~@#~/\\   #
                                     ##  @@# @##@  `..@,                                 
                                   @#/  #@#   _##     `\\                                 
                                 @##;  `#~._.' ##@      \\_                               
                               .-#/           @#@#@--,_,--\\                              
                              / `@#@..,     .~###'         `~.                           
                            _/         `-.-' #@####@          \\                          
                         __/     &^^       ^#^##~##&&&   %     \\_                        
                        /       && ^^      @#^##@#%%#@&&&&  ^    \\                       
                      ~/         &&&    ^^^   ^^   &&&  %%% ^^^   `~._                   
                   .-'   ^^    %%%. &&   ___^     &&   && &&   ^^     \\                  
                  /    ^^^ ___&&& X & &&   &   ^ ___ %____&& . ^^^^^   `~.""";
        }
        else if (item.contains("hell")){
            res = """
                          _.---**""**-.       
                  ._   .-'           /|`.     
                   \\`.'             / |  `.   
                    V              (  ;    \\  
                    L       _.-  -. `'      \\ 
                   / `-. _.'       \\         ;
                  :            __   ;    _   |
                  :`-.___.+-*"': `  ;  .' `. |
                  |`-/     `--*'   /  /  /`.\\|
                  : :              \\    :`.| ;
                  | |   .           ;/ .' ' / 
                  : :  / `             :__.'  
                   \\`._.-'       /     |      
                    : )         :      ;      
                    :----.._    |     /       
                   : .-.    `.       /        
                    \\     `._       /         
                    /`-            /          
                   :             .'           
                    \\ )       .-'             
                     `-----*"'""";
        }
        else if (item.contains("graveyard")){
            res = """
                                    _|_
                                     |
                                    / \\
                                   //_\\\\
                                  //(_)\\\\
                                   |/^\\| 
                         ,%%%%     // \\\\    ,@@@@@@@,
                       ,%%%%/%%%  //   \\\\ ,@@@\\@@@@/@@,
                   @@@%%%\\%%//%%%// === \\\\ @@\\@@@/@@@@@
                  @@@@%%%%\\%%%%%// =-=-= \\\\@@@@\\@@@@@@;%#####,
                  @@@@%%%\\%%/%%//   ===   \\\\@@@@@@/@@@%%%######,
                  @@@@@%%%%/%%//|         |\\\\@\\\\//@@%%%%%%#/####
                  '@@@@@%%\\\\/%~ |         | ~ @|| %\\\\//%%%#####;
                    @@\\\\//@||   |  __ __  |    || %%||%%'######
                     '@||  ||   | |  |  | |    ||   ||##\\//####
                       ||  ||   | | -|- | |    ||   ||'#||###'
                       ||  ||   |_|__|__|_|    ||   ||  ||
                       ||  ||_/`  =======  `\\__||_._||  ||
                     __||_/`      =======            `\\_||___""";
        }
        else if (item.contains("ghost")){
            res = """
                       .-.
                     .'   `.
                     :g g   :
                     : o    `.
                    :         ``.
                   :             `.
                  :  :         .   `.
                  :   :          ` . `.
                   `.. :            `. ``;
                      `:;             `:'
                         :              `.
                          `.              `.     .
                            `'`'`'`---..,___`;.-'""";
        }
        else if (item.contains("barn")){
            res = """
                                               +&-
                                             _.-^-._    .--.
                                          .-'   _   '-. |__|
                                         /     |_|     \\|  |
                                        /               \\  |
                                       /|     _____     |\\ |
                                        |    |==|==|    |  |
                    |---|---|---|---|---|    |--|--|    |  |
                    |---|---|---|---|---|    |==|==|    |  |
                       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^""";
        }      
        else if (item.contains("sea")){
            res = """
                                          ___
                                       .-'   `'.
                                      /         \\
                                      |         ;
                                      |         |           ___.--,
                             _.._     |0) ~ (0) |    _.---'`__.-( (_.
                      __.--'`_.. '.__.\\    '--. \\_.-' ,.--'`     `""`
                     ( ,.--'`   ',__ /./;   ;, '.__.'`    __
                     _`) )  .---.__.' / |   |\\   \\__..--""  \"""--.,_
                    `---' .'.''-._.-'`_./  /\\ '.  \\ _.-~~~````~~~-._`-.__.'
                          | |  .' _.-' |  |  \\  \\  '.               `~---`
                           \\ \\/ .'     \\  \\   '. '-._)
                            \\/ /        \\  \\    `=.__`~-.
                            / /\\         `) )    / / `"".`\\
                      , _.-'.'\\ \\        / /    ( (     / /
                       `--~`   ) )    .-'.'      '.'.  | (
                              (/`    ( (`          ) )  '-;
                               `      '-;         (-'""";
        }
        else if (item.contains("spider")){
            res = """
                             ____                      ,
                            /---.'.__             ____//
                                 '--.\\           /.---'
                            _______  \\\\         //
                          /.------.\\  \\|      .'/  ______
                         //  ___  \\ \\ ||/|\\  //  _/_----.\\__
                        |/  /.-.\\  \\ \\:|< >|// _/.'..\\   '--'
                           //   \\'. | \\'.|.'/ /_/ /  \\\\
                          //     \\ \\_\\/" ' ~\\-'.-'    \\\\
                         //       '-._| :H: |'-.__     \\\\
                        //           (/'==='\\)'-._\\     ||
                        ||                        \\\\    \\|
                        ||                         \\\\    '
                        |/                          \\\\
                                                     ||
                                                     ||
                                                     \\\\
                                                      '""";
        }
        else if (item.contains("someone")){
            res = """
                              *********
                             *************
                            *****     *****
                           ***           ***
                          ***             ***
                          **    0     0    **
                          **               **                  ____
                          ***             ***             //////////
                          ****           ****        ///////////////  
                          *****         *****    ///////////////////
                          ******       ******/////////         |  |
                        *********     ****//////               |  |
                     *************   **/////*****              |  |
                    *************** **///***********          *|  |*
                   ************************************    ****| <=>*
                  *********************************************|<===>* 
                  *********************************************| <==>*
                  ***************************** ***************| <=>*
                  ******************************* *************|  |*
                  ********************************** **********|  |*  
                  *********************************** *********|  |""";
        }
        else if (item.contains("waterfall")){
            res = """
                                    _.._
                     _________....-~    ~-.______
                  ~~~                            ~~~~-----...___________..--------
                                                             |   |     |
                                                             | |   |  ||
                                                             |  |  |   |
                                                             |'. .' .`.|
                  ___________________________________________|0oOO0oO0o|____________
                   -          -         -       -      -    / '  '. ` ` \\    -    -
                        --                  --       --   /    '  . `   ` \\    --
                  ---            ---          ---       /  '                \\ ---
                       ----               ----        /       ' ' .    ` `    \\  ----
                  -----         -----         ----- /   '   '        `      `   \\
                       -----          ------     /          '    . `     `    `  \\
                              -------           /  '    '      '      `
                                      --------/     '     '   '""";
        }    
        else if (item.contains("cliff")){
            res = """
                         _.-'```\"""``"-._
                      /"`                '.
                      `':.,_               "._
                           \\`'-._             `'-._
                            \\    `:._              `'-._          _
                            |      \\ `:_                `"--"--"``
                            |       \\   `:_
                            |      :|    \\ '.
                            |       |     |  `:_
                            |       |:    |     `:_
                            |:      |     |
                            |       |
                            |
                            |""";
        }
        else if (item.contains("cave")){
            res = """
                                       /   \\              /'\\       _
                   \\_..           /'.,/     \\_         .,'   \\     / \\_
                       \\         /            \\      _/       \\_  /    \\     _
                        \\__,.   /              \\    /           \\/.,   _|  _/ \\
                             \\_/                \\  /',.,''\\      \\_ \\_/  \\/    \\
                                              _  \\/   /    ',../',.\\    _/      \\
                                /           _/m\\  \\  /    |         \\  /.,/'\\   _\\
                              _/           /MMmm\\  \\_     |          \\/      \\_/  \\
                             /      \\     |MMMMmm|   \\__   \\          \\_       \\   \\_
                                     \\   /MMMMMMm|      \\   \\           \\       \\    \\
                                      \\  |MMMMMMmm\\      \\___            \\_      \\_   \\
                                       \\|MMMMMMMMmm|____.'  /\\_            \\       \\   \\_
                                       /'.,___________...,,'   \\            \\   \\        \\
                                      /       \\          |      \\    |__     \\   \\_       \\
                                    _/        |           \\      \\_     \\     \\    \\       \\_
                                   /                               \\     \\     \\_   \\        \\
                                                                    \\     \\      \\   \\__      \\
                                                                     \\     \\_     \\     \\      \\
                                                                      |      \\     \\     \\      \\ 
                                                                       \\            |            \\  """;
        }
        else if (item.contains("tree house")){
            res = """
                                                               vvv^^^^vvvvv                      
                                                           vvvvvvvvv^^vvvvvv^^vvvvv              
                                                  vvvvvvvvvvv^^^^^^^^^^^^^vvvvv^^^vvvvv          
                                              vvvvvvv^^^^^^^^^vvv^^^^^^^vvvvvvvvvvv^^^vvv        
                                            vvvv^^^^^^vvvvv^^^^^^^vv^^^^^^^vvvv^^^vvvvvv         
                                           vvv^^^^^vvvv^^^^^^vvvvv^^vvvvvvvvv^^^^^^vvvvv^        
                                            vvvvvvvvvv^^^v^^^vvvvvv^^vvvvvvvvvv^^^vvvvvvvvv      
                                            ^vv^^^vvvvvvv^^vvvvv^^^^^^^^vvvvvvvvv^^^^^^vvvvvv    
                                               ^vvvvvvvvv^^^^vvvvvv^^^^^^vvvvvvvv^^^vvvvvvvvvv^v 
                                                  ^^^^^^vvvv^^vvvvv^vvvv^^^v^^^^^^vvvvvv^^^^vvvvv
                                           vvvv^^vvv^^^vvvvvvvvvv^vvvvv^vvvvvv^^^vvvvvvv^^vvvvv^ 
                                          vvv^vvvvv^^vvvvvvv^^vvvvvvv^^vvvvv^v##vvv^vvvv^^vvvvv^v
                                            ^vvvvvv^^vvvvvvvv^vv^vvv^^^^^^_____##^^^vvvvvvvv^^^^ 
                                               ^^vvvvvvv^^vvvvvvvvvv^^^^/\\@@@@@@\\#vvvv^^^        
                                                    ^^vvvvvv^^^^^^vvvvv/__\\@@@@@@\\^vvvv^v        
                                                        ;^^vvvvvvvvvvv/____\\@@@@@@\\vvvvvvv       
                                                        ;      \\_  ^\\|[  -:] ||--| | _/^^        
                             .vv^^vv.                  ;         \\   |[   :] ||_/| |/            
                  ^v.      ^vvvvvvv^^v                 ;          \\\\ |[___:]______/              
                  vvv^    ^v^^^^vvv^^^vv^               ;          \\   ;=; /                     
                  v^^^^  ^vvvvvv ^vv^^^vv^v^            ;           |  ;=;|                      
                  vvvvv^^vvv^^vv^vv^^^vvv^^v^          ;           ()  ;=;|                      
                  vvvvvv^v^^^vv^^vvv^^^^^vv^^         (()           || ;=;|                      
                  vvvvvv^vvvvvvv^vvvvvvvvv^^                       / / \\;=;\\     """;
        }
        else if (item.contains("tree")){
            res = """
                                v .   ._, |_  .,
                             `-._\\/  .  \\ /    |/_
                                 \\\\  _\\, y | \\//
                           _\\_.___\\\\, \\\\/ -.\\||
                             `7-,--.`._||  / / ,
                             /'     `-. `./ / |/_.'
                                       |    |//
                                       |_    /
                                       |-   |
                                       |   =|
                                       |    |
                  --------------------/ ,  . \\--------._""";
        }
        else if (item.contains("forest")){
            res = """
                         ^  ^  ^   ^      ___I_      ^  ^   ^  ^  ^   ^  ^
                        /|\\/|\\/|\\ /|\\    /\\-_--\\    /|\\/|\\ /|\\/|\\/|\\ /|\\/|\\
                        /|\\/|\\/|\\ /|\\   /  \\_-__\\   /|\\/|\\ /|\\/|\\/|\\ /|\\/|\\
                        /|\\/|\\/|\\ /|\\   |[]| [] |   /|\\/|\\ /|\\/|\\/|\\ /|\\/|\\""";
        }      
        return res+"\n"+item;
    }
}
