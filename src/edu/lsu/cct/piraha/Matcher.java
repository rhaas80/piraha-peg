package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;


public class Matcher extends Group {
	int maxId;
	private int textPos;
	int maxTextPos;
	Pattern pattern;
	public String lookup, maxLookup;
	public boolean didMatch;
	
	public boolean find() {
		if(end == 0)
			return find(0);
		else if(begin == end)
			return find(end+1);
		else
			return find(end);
	}
	public boolean find(int pos) {
		for(int i=pos;i<text.length();i++) {
			if(match(i))
				return true;
		}
		return false;
	}
	public boolean matches() {
		return match(0);
	}
	final public int getTextPos() {
		return textPos;
	}
	final public void setTextPos(int newTextPos) {
		if(newTextPos > maxTextPos) {
			maxTextPos = newTextPos;
			maxLookup = lookup;
		}
		textPos = newTextPos;
	}
	final public void incrTextPos(int incr) {
		setTextPos(getTextPos()+incr);
	}
	public boolean match(int pos) {
		textPos = pos;
		maxTextPos = pos;
		savedMatches = new Stack<LinkedList<Group>>();
		subMatches = new LinkedList<Group>();
		if(matchAll(pattern,this)) {
			begin = pos;
			end = textPos;
			didMatch = true;
			return true;
		}
		begin = end = -1;
		didMatch = false;
		return false;
	}
	static boolean matchAll(Pattern pattern, Matcher m) {
		/*for(Pattern x = pattern;x != null;x = x.next) {
			if(!x.match(m))
				return false;
		}
		return true;*/
		return pattern.match(m);
	}
	
	public void dumpMatches() {
		dumpMatches(text,subMatches);
	}
	public String getText() {
		return text;
	}
	void dumpMatches(String text, LinkedList<Group> matches) {
		dumpMatches(text,matches,DebugOutput.out);
	}
	static void dumpMatches(String text,LinkedList<Group> matches,DebugOutput out) {
		for(Group match : matches) {
			dumpMatches(text, match, out);
		}
	}
	static void dumpMatches(String text, Group match, DebugOutput out) {
		//out.println(match.lookup+" ["+match.before+" to "+match.after+"] \""+text.substring(match.before,match.after)+"\"");
		out.println("<"+match.getPatternName()+">");
		out.indent++;
		try {
			if(match.groupCount()==0)
				out.println("<contents>"+xmltext(text.substring(match.getBegin(),match.getEnd()))+"</contents>");
			dumpMatches(text,match.subMatches,out);
		} finally {
			out.indent--;
			out.println("</"+match.patternName+">");
		}
	}
	private static String xmltext(String str) {
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<str.length();i++) {
			char c = str.charAt(i);
			if(c == '<')
				sb.append("&lt;");
			else if(c == '>')
				sb.append("&gt;");
			else if(c == '&')
				sb.append("&amp;");
			else if(c == '"')
				sb.append("&quot;");
			else if(c <= 13 || c > 127) 
				sb.append("&#"+(int)c+";");
			else
				sb.append(c);
		}
		return sb.toString();
	}
	static void dumpMatches(String text, Group match) {
		dumpMatches(text,match,DebugOutput.out);
	}
	
	public static class Near {
		String text,rule;
		int lineNum=1, posInLine, startOfLine, endOfLine;
		public String toString() {
			String line = text.substring(startOfLine,endOfLine);
			if(rule != null)
				return ""+lineNum+" in {"+rule+"}: '"+(line.substring(0,posInLine)+"|"+line.substring(posInLine)).trim()+"'";
			else
				return ""+lineNum+": '"+(line.substring(0,posInLine)+"|"+line.substring(posInLine)).trim()+"'";
		}
	}
	
	public Near near() {
		Near near = new Near();
		near.text = text;
		near.rule = maxLookup;
		for(int i=0;i<text.length() && i<maxTextPos;i++) {
			if(text.charAt(i)=='\n') {
				near.lineNum++;
				near.startOfLine = i;
			}
		}
		near.posInLine = maxTextPos - near.startOfLine;
		for(near.endOfLine = maxTextPos; near.endOfLine < text.length();near.endOfLine++)
			if(text.charAt(near.endOfLine) == '\n')
				break;
		return near;
	}
	
	static class Mapping {
		int from, delta;
		Mapping(int from,int delta) {
			this.from = from;
			this.delta = delta;
		}
		public String toString() {
			return "["+from+" += "+delta+"]";
		}
	}
	List<Mapping> mappings = null;
	StringBuffer sb = null;
	int lastAppend = 0, lastDelta = 0;
	public void startReplacement() {
		sb = new StringBuffer();
		mappings = new ArrayList<Mapping>();
		lastAppend = 0;
		lastDelta = 0;
	}
	private void addMapping(int from,int to) {
		if(from < 0) return;
		int delta = to - from;
		if(delta != lastDelta) {
			mappings.add(new Mapping(from,delta));
			lastDelta = delta;
		}
	}
	public void appendReplacement(String s) {
		sb.append(text.substring(lastAppend,begin));
		addMapping(begin,sb.length());
		addMapping(end-1,sb.length()+s.length()-1);
		sb.append(s);
		lastAppend = end;
	}
	public String appendTail() {
		if(lastAppend < text.length())
			sb.append(text.substring(lastAppend));
		lastAppend = text.length();
		return sb.toString();
	}
	public int mapping(int from) {
		int delta = 0;
		for(Mapping m : mappings) {
			if(m.from > from)
				break;
			delta = m.delta;
		}
		//System.out.println("delta["+from+"]="+delta);
		return from+delta;
	}
	public Pattern getPattern() {
		return pattern;
	}
	/*
	public static void main(String[] args) {
		Grammar g = new Grammar();
		g.compile("test","short");
		Matcher m = g.matcher("test", "short short short");
		m.startReplacement();
		while(m.find()) {
			m.appendReplacement("long");
		}
		String result = m.appendTail();
		System.out.println(result);
		System.out.println(m.mappings);
		for(int i=0;i<m.text.length();i++) {
			System.out.println(m.text.charAt(i)+" :> "+result.charAt(m.mapping(i)));
		}
	}
	*/
}