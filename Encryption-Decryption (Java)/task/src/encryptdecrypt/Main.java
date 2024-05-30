package encryptdecrypt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.HashMap;

abstract class StrategieReference{
    int shift = 0;

    public void setShift(int shift){this.shift = shift;}

    public abstract char getDecryptedCharacter(char c);

    public String decryptMessage(String characters) {
        return decryptMessage(characters.toCharArray());
    }

    public String decryptMessage(char[] characters) {
        char[] newChars = new char[characters.length];

        for (int i = 0; i < characters.length; i++) {
            newChars[i] = getDecryptedCharacter(characters[i]);
        }
        return new String(newChars);
    }

    public abstract char getEncryptedCharacter(char c);

    public String encryptMessage(char[] characters) {
        char[] newChars = new char[characters.length];

        for (int i = 0; i < characters.length; i++) {
            newChars[i] = getEncryptedCharacter(characters[i]);
        }
        return new String(newChars);
    }

    public String encryptMessage(String characters) {
        return encryptMessage(characters.toCharArray());
    }

}

class StrategyReferenceGBAlpha extends StrategieReference{
    String reference = "abcdefghijklmnopqrstuvwxyz";

    public char getDecryptedCharacter(char c){
        int newpos = 0;
        char newc = c;
        if (c >= 'a' && c <= 'z') {
            int pos = c - 'a';
            newpos = (reference.length() + pos - shift) % reference.length();
            if(newpos < 0) newpos+=reference.length();
            newc = (char) ('a' + newpos);
        }else if (c >= 'A' && c <= 'Z') {
            int pos = c - 'A';
            newpos = (reference.length() + pos - shift) % reference.length();
            if(newpos < 0) newpos+=reference.length();
            newc = (char) ('A' + newpos);
        }
        return newc;
    }

    public char getEncryptedCharacter(char c){
        int newpos = 0;
        char newc = c;
        if (c >= 'a' && c <= 'z') {
            int pos = c - 'a';
            newpos = (pos + shift) % reference.length();
            if (newpos < 0) newpos = reference.length() + newpos;
            newc = (char) ('a' + newpos);
        }else if (c >= 'A' && c <= 'Z') {
            int pos = c - 'A';
            newpos = (pos + shift) % reference.length();
            if (newpos < 0) newpos = reference.length() + newpos;
            newc = (char) ('A' + newpos);
        }
        return newc;
    }
}

class StrategyReferenceUnicode extends StrategieReference{

    public char getDecryptedCharacter(char c){
        int newpos = 0;
        newpos = (0xFFFF + c - shift) % 0xFFFF;
        if(newpos < 0) newpos+=0xFFFF;
        return (char) newpos;
    }

    public char getEncryptedCharacter(char c){
        int newpos = 0;
        newpos = (c + shift) % 0xFFFF;
        if (newpos < 0) newpos += 0xFFFF;
        return (char) newpos;
    }
}

abstract class StrategyInput{
    protected String source;
    abstract String getMessage();

    void setSource(String source){this.source = source;};
}

abstract class StrategyOutput{
    protected String target;
    abstract void outputMessage(String message);

    void setTarget(String target){this.target = target;};
}

class StrategyInputFile extends StrategyInput{
    public String getMessage() {
        try {
            return new String(Files.readAllBytes(new File(source).toPath()));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}

class StrategyInputStd extends StrategyInput{
    public String getMessage() {return source;}
}

class StrategyOutputFile extends StrategyOutput{
    void outputMessage(String message){
        try {
            Files.write(Paths.get(target), message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class StrategyOutputStd extends StrategyOutput{
    void outputMessage(String message) {
        System.out.println(message);
    }
}

class Context{
    private StrategieReference strategy;
    private CommandLineParser parser;
    private StrategyInput source;
    private StrategyOutput target;

    public Context(CommandLineParser parser){
        this.parser = parser;
        setStrategy();
        setShift();
        setInput();
        setOutput();
    }

    private void setStrategy() {
        String alg = parser.getValue("-alg");
        if(alg != null && alg.equals("unicode")){
            strategy = new StrategyReferenceUnicode();
        }else {
            this.strategy = new StrategyReferenceGBAlpha();
        }
    }

    private String executeDecryption(String characters) {
        return strategy.decryptMessage(characters);
    }

    private String executeEncryption(String characters) {
        return strategy.encryptMessage(characters);
    }

    public void executeTask(){
        switch(parser.getValue("-mode")){
        case "enc":
            target.outputMessage(executeEncryption(source.getMessage()));
            break;
        case "dec":
            target.outputMessage(executeDecryption(source.getMessage()));
            break;
        }
    }

    void setInput(){
        String data = parser.getValue("-data");
        String input = parser.getValue("-in");

        if(data == null && input != null){
            source = new StrategyInputFile();
            source.setSource(parser.getValue("-in"));
        }
        else{
            source = new StrategyInputStd();
            source.setSource(parser.getValue("-data") == null ? "" : parser.getValue("-data"));
        }
    }

    void setOutput(){
        String output = parser.getValue("-out");
        if (output == null) {
            target = new StrategyOutputStd();
        } else {
            target = new StrategyOutputFile();
            target.setTarget(output);
        }
    }

    private void setShift() {
        strategy.setShift(Integer.parseInt(parser.getValue("-key")));
    }
}

class StructParameter{
    String defaultValue = "";
    List<String> possibleValues = new ArrayList<>();

    public StructParameter(String defaultValue, List<String> possibleValues){
        this.defaultValue = defaultValue;
        this.possibleValues = possibleValues;
    }
}

class CommandLineParser{
    private HashMap<String, String> parameters = new HashMap<>();
    private HashMap<String, StructParameter> defaults = new HashMap<>();

    public void addParameter(String key, String defaultValue){
        defaults.put(key, new StructParameter(defaultValue, null));
    }

    public void addParameter(String key, String defaultValue, List<String>possibleValues){
        defaults.put(key, new StructParameter(defaultValue, possibleValues));
    }

    public void add(String key, String value){
        parameters.put(key, checkValue(key, value));
    }

    public void add(String[] args){
        for (int i = 0; i < args.length; i += 2) {
            if(i+1 >= args.length) return;
            add(args[i], args[i + 1]);
        }
    }

    private String checkValue(String key, String value){
        StructParameter param = defaults.get(key);
        if(param == null) return value;
        if(param.possibleValues != null){
            if(param.possibleValues.contains(value)) return value;
            return param.defaultValue;
        }
        if(value.isEmpty()) return param.defaultValue;
        return value;
    }

    public String getValue(String key){
        return parameters.get(key);
    }
}

public class Main {
    public static void main(String[] args) {
        CommandLineParser parser = new CommandLineParser();
        parser.addParameter("-mode", "enc", List.of("enc", "dec"));
        parser.addParameter("-key", "0");
        parser.addParameter("-data", "");
        parser.addParameter("-in", "");
        parser.addParameter("-alg", "unicode", List.of("unicode", "shift"));
        parser.addParameter("-out", "");
        parser.add(args);

        Context context = new Context(parser);
        context.executeTask();
    }
}
