/*
Author:		Richard Seddon
Date:		2-4-17
File:		TwoPassAssembler.java
Course:		Cosc 4316, COmpiler Design and Construction
Professor:	Dr. T. McGuire
Details:	This program is a simple two pass assembler for the assembly language specified in Lab 1.
			The first pass creates the symbol table, and the second pass uses the symbol table to generate a binary file.
			Memory is assumed to be word accessible so addresses are in terms of words.
			The binary file is generated in Big Endian format.
			Identifiers are allowed to be from 1 to 20 characters long, starting with a letter followed by letters and digits.
			Case is ignored for ops and variables.
			Variables are only declared in the .data section.
			Labels are only declared in the .code section.
Compile:    In cmd, in correct directory, "javac TwoPassAssembler.java"
Run:		In cmd, "java TwoPassAssembler < (inputfile) > (outputfile)"
*/
import java.io.*;
import java.util.*;
public class TwoPassAssembler {
	public static void main(String[] args) {

		int codeLocCount = 0;	//Keeps track of code location
		int dataLocCount = 0;	//Keeps track of data location
		int lineNumber = 0;		//keeps track of line number
		int literal;			//to hold literal ints from code
		String token = "";		//A string to temporarily store tokens
		String line;			//A string to store a line of input to parse
		char buffer;			//Char to hold char by char input from stream
		boolean test = true;	//A boolean for various tests
		boolean success = true; //another test
		int operand = 0;		//to check whether a command has an operand or not

		ArrayList<String[]> symbolTable = new ArrayList<String[]>(); //The symbol table, a table of tuples in the form {name, type, value(for op codes)/location(for variables)}

		String[] ops = //ops is a list of the op codes to put in the symbol table
			{"Halt", "Push", "Rvalue", "Lvalue", "Pop", "Sto", "Copy", "Add", "Sub", "Mpy", "Div", "Mod", "Neg", "Not", "Or", //Note: Sto is equal to :=, I will account for this in translation/parsing
			"And", "Eq", "Ne", "Gt", "Ge", "Lt", "Le", "Label", "Goto", "Gofalse", "Gotrue", "Print", "Read", "Gosub", "Ret"};


		for (int i = 0; i < 30; i++) {	//This for loop adds the op codes to the symbol table
			symbolTable.add(new String[] {ops[i], "OP", Integer.toString(i)});
		}//end of op code for loop
		symbolTable.add(new String[] {":=", "OP", "5"}); //To account for := being the same as Sto

		Scanner sc = new Scanner(System.in);

		byte[] output = new byte[2]; //this is an array of bytes that will be used to output to a binary file
		ArrayList<String> file = new ArrayList<String>();//an array to hold the file for the second pass so that raw input can be used

		//first pass, generate symbol table
		line = sc.nextLine();
		line = line.replaceAll("\\s+", "");
		file.add(line);
		if (!(line.equalsIgnoreCase("section.data")))
			System.out.println("Error: Section.data expected");

		line = sc.nextLine();
		line = line.replaceAll("\\s+", "");
		file.add(line);
		//System.out.println(line);
		buffer = line.charAt(0);
		while (!(line.equalsIgnoreCase("section.code"))) { 	//makes sure only variables from the data section are added to the symbol table
			for (int i = 1; i < line.length(); i++) {		//accepts letters and digits for a token until something else is found
				if (i == 1 && Character.isDigit(buffer)) {	//Makes sure the first character isn't a digit
					System.out.println("Error: Identifier must start with a letter");
					break;
				}//end of if
				if (Character.isLetterOrDigit(buffer)) {	//if the character is okay, add it to the token
					token = token + Character.toString(buffer);
					buffer = line.charAt(i);
				}//end of if
				else {	//if the character is not okay, we've presumably got a full token. Test it and store it.
					//System.out.println(token);
					if (idTest(token)) {//valid token?
						test = true;
						for(int k = 0; k < symbolTable.size(); k++) {//it's valid, but is it in the symbol table already?
							if (symbolTable.get(k)[0].equalsIgnoreCase(token)) {//check the token against the symbol table
								System.out.println("Error: identifier matches either previous identifier or OP");
								test = false;
								break;
							}//end of if
						}//end of for
						if (test) {//not in the symbol table? Great, let's add it
							if (line.contains(":word")) {//but only if the grammar is right!
								symbolTable.add(new String[] {token, "WORD", Integer.toString(dataLocCount)});
								dataLocCount++;
							}//end of if
							else {//if the grammar is wrong, let the user know
								System.out.println("Error: ':	word' expected");
							}//end of else
					}//end of else
					else//if the token isn't valid, let the user know
						System.out.println("Error: Invalid identifier");
						break;
					}//end of if
				}//end of else
			}//end of for
			line = sc.nextLine();
			line = line.replaceAll("\\s+", "");
			file.add(line);
			buffer = line.charAt(0);
			token = "";
			//and do it all again until we're done with the .data section
		}//end of while, end of variable declaration

		codeLocCount = -1;//so that codeLocCount starts at line 0
		while (!(line.contains("HALT"))) {//if not end of file, keep going
			line = sc.nextLine();
			file.add(line);
			codeLocCount++;
			if (line.contains("LABEL")) {//looking for a label
				line = line.replace("LABEL", "");
				line = line.replaceAll("\\s+", "");
				line = line.replaceAll("\\t+", "");

				if (idTest(line)) {//valid token?
					test = true;
					for(int k = 0; k < symbolTable.size(); k++) {//it's valid, but is it in the symbol table already?
						if (symbolTable.get(k)[0].equalsIgnoreCase(line)) {//check the token against the symbol table
							System.out.println("Error: identifier matches either previous identifier or OP");
							test = false;
							break;
						}//end of if
					}//end of for
					if (test) {//not in the symbol table? Great, let's add it
						symbolTable.add(new String[] {line, "LABEL", Integer.toString(codeLocCount)});
					}//end of if
					else {
						System.out.println("Error: Invalid Label in section.code line " + codeLocCount);
					}//end else
				}//end of if
			}//end of if
		}//end of while

		/*
		System.out.println(file);
		for (int o = 0; o < symbolTable.size(); o++) {
			for(int v = 0; v < 3; v++) {
				System.out.println(symbolTable.get(o)[v]);
			}//end of for
		}//end of for
		*/

		//now go through and generate binary file
		for (int y = 0; y < file.size(); y++) {//this loop skips to the section.code
			line = file.get(y);
			if (line.contains(".code")) {
				lineNumber = y + 1;
				break;
			}//end of if
		}//end of for

		for (int y = lineNumber; y < file.size(); y++) {//this loop goes through the file and generates binary
			test = false;
			line = file.get(y);
			//System.out.println(line);
			token = getToken(line);
			//System.out.println(token);
			line = line.replace(token, "");
			//System.out.println(line);
			//int dumb = sc.nextInt();
				operand = 0;
				while ((!(token.equals(""))) && operand < 2) {
					success = false;
					for (int k = 0; k < symbolTable.size(); k++) {//this loop checks the symbol table for the token
						if (symbolTable.get(k)[0].equalsIgnoreCase(token)) {
							output[0] = 0x00;
							if (symbolTable.get(k)[1].equalsIgnoreCase("label")) {
								output[1] = 0x00;
							}//end if
							else {
								output[1] = (byte)Integer.parseInt(symbolTable.get(k)[2]);
							}//end else
							try {
								System.out.write(output);
							}//end of try
							catch(IOException e) {
								System.out.println("Error in byte output");
							}//end of catch
							success = true;
							break;
						}//end of if
					}//end of for
					if (!(success)) {
						for (int l = 0; l < token.length(); l++) {
							if (!(Character.isDigit(token.charAt(l)))) {
								test = false;
								break;
							}//end of if
								test = true;
						}//end of for
						if (test) {
							literal = Integer.parseInt(token);
							output[0] = 0x00;
							output[1] = (byte)literal;
							try {
								System.out.write(output);
							}//end of try
							catch(IOException e) {
								System.out.println("Error in byte output");
							}//end of catch
							success = true;
						}//end of if test
					}//end of if not success
					if  (!(success))
						System.out.println("Error: symbol " + token + " not found from line " + y);
					line = line.replaceAll("\\s+", "");
					//System.out.println(line);
					token = getToken(line);
					line = line.replace(token, "");
					//System.out.println(token);
					//System.out.println(line);
					operand++;
				}//end of while
				if (operand != 2) {
					output[0] = 0x00;
					output[1] = 0x00;
					try {
						System.out.write(output);
					}//end of try
					catch(IOException e) {
						System.out.println("Error in byte output");
					}//end of catch
				}//end of if
		}//end of outer for
	}//end of main

	public static boolean idTest(String id) {//idtest tests whether the given token is a valid identifier
		boolean valid = true;
		//System.out.println(id);
		char x = id.charAt(0);
		if ((!(Character.isLetter(x)) || id.length() > 20)) {
			valid = false;
		}//end of if
		else {
			for (int i = 1; i < id.length(); i++) {
				if (!(Character.isLetterOrDigit(id.charAt(i)))) {
					valid = false;
					break;
				}//end of if
			}//end of for
		}//end of if
		return valid;
	}//end of idtest

	public static String getToken(String line) {//takes a string and determines a token from it
		String tok = "";
		int spot = 0;
		if (line.equals(""))
			return "";
		for (int i = 0; i < line.length(); i++) {//this loop skips any initial whitespace
			if (Character.isLetterOrDigit(line.charAt(i))) {
				spot = i;
				break;
			}//end if
		}//end of for
		for (int q = spot; q < line.length(); q++) {
			if (Character.isLetterOrDigit(line.charAt(q)))
				tok = tok + line.charAt(q);
			else
				break;
		}//end of for
		return tok;
	}//end of getToken
}//End of TwoPassAssembler

/*
Note: After getting most of the way through this I've realized this is probably a bad way to go about this.
I didn't know that the name of the file could be input and then used, though it seems obvious now. That is
why I decided to use raw input, and why the whole thing seems so awkward. I will probably try and redo this
in a much better and more efficient way, but that may have to be after I turn it in this way. For now, I'm
just going to keep going like this until I finish/
*/