package com.gmail.javad.mnjd;

import java.util.regex.Pattern;

public class Rule {
	Pattern regex;
	String replacerchar;
	
	public Rule(String patt, String Replacer){
		this.regex = Pattern.compile(patt);
		replacerchar = Replacer;
	}

}
