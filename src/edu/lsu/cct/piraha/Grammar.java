package edu.lsu.cct.piraha;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Grammar {
	Map<String,Pattern> patterns = new HashMap<String,Pattern>();
	Map<String,Grammar> grammars = null;
	public void importGrammar(String name,Grammar g) {
		if(grammars == null)
			grammars = new HashMap<String,Grammar>();
		if(grammars.containsKey(name)) {
			throw new ParseException("Grammar '"+name+"' is already defined");
		}
		grammars.put(name, g);
	}
	
	public Pattern compile(String name,String pattern) {
		if(name.indexOf(':') >= 0)
			throw new ParseException("Illegal character ':' in pattern name");
		if(patterns.containsKey(name))
			throw new ParseException("Rule '"+name+"' is already defined.");
		//System.out.print("compile: "+name+" = "+pattern);
		//System.out.println();
		Compiler c = new Compiler(this, name,pattern);
		Pattern pat = c.getPattern(false,false);
		patterns.put(name, pat);
		return pat;
	}
	
	// TODO: Make import work
	// TODO: Make a decompile work
	// TODO: Add "Grammar name" sections
	// TODO: Make a more accurate rule to match regex's
    public void compileFile(File filename) throws IOException {
        String contents = readContents(filename).trim();
        compileFile(contents);
    }
    public void compileFile(InputStream in) throws IOException {
    	StringBuilder sb = new StringBuilder();
    	BufferedInputStream bin = new BufferedInputStream(in);
    	InputStreamReader isr = new InputStreamReader(bin);
    	int c=0;
    	while((c = isr.read()) != -1)
    		sb.append((char)c);
    	compileFile(sb.toString());
    }
    public void compileFile(String contents) throws IOException {
        Grammar pegRules = new Grammar();
        pegRules.compile("name","[a-zA-Z0-9_:]+");
        pegRules.compile("import", "import");
        pegRules.compile("filename","[^\"]+");//"\\\\[^]|[^\\\\\"]+");
        pegRules.compile("regex","((?!\n{name}white}=|{import}{-white}\"{filename}\"{-white}as\\b{-white}{name})(\\[(\\\\[^]|[^\\]\\\\])*\\]|\\\\[^]|[^ \t\r\n\b]))+");
        pegRules.compile("white","([ \t\r\b]|\n(?=[\n#])|\n[ \t\r\b]|#[^\n]*)*");
        //pegRules.compile("white","([ \t\r\b\n]|#[^\n]*)*");
        pegRules.compile("rule","({name}{-white}={-white}({regex}{-white})*"+/*|{import}{-white}\"{filename}\"{-white}as\\b{-white}{name}*/")");
        pegRules.compile("rules","^{-white}((^|\n){rule})*$");
        Matcher pegMatcher = pegRules.matcher("rules", contents);
        if(!pegMatcher.matches()) {
            throw new ParseException("Syntax error near line "+pegMatcher.near()+" : "+
                    pegMatcher.text.substring(0,pegMatcher.maxTextPos)+">|<"+
                    pegMatcher.text.substring(pegMatcher.maxTextPos));
        }
        //pegMatcher.dumpMatches();
        for(Group match : pegMatcher.subMatches) {
        	Group value = match.getMatch(0);
            String name = value.getPatternName();
            if("name".equals(name)) {
            	//System.out.println("start: "+match.substring());
            	StringBuilder sb = new StringBuilder();
            	for(int i=1;i<match.groupCount();i++) {
            		if(i > 1) sb.append("{-skipper}");
            		if(true) {//match.getPatternName().equals("regex")) {
            			//System.out.println("  "+match.getPatternName()+": "+match.getMatch(i).substring());
            			sb.append(match.getMatch(i).substring());
            		}
            	}
            	compile(value.substring(),sb.toString());
            } else if("import".equals(name)) {
            	String impFile = match.getMatch(1).substring();
            	if(!new File(impFile).exists())
            		throw new IOException("no such file "+impFile);
            	String impName = match.getMatch(2).substring();
            	if(grammars == null || !grammars.containsKey(impName)) {
                	Grammar subGrammar = new Grammar();
                	subGrammar.compileFile(new File(impFile));
                	importGrammar(impName, subGrammar);
            	}
            }
        }
    }

	public void diag(DebugOutput out) {
		DebugVisitor dv = new DebugVisitor(out);
		for(Map.Entry<String, Pattern> entry : patterns.entrySet()) {
			out.println(entry.getKey()+" => ");
			dv.out.indent+=2;
			entry.getValue().visit(dv);
			dv.out.indent-=2;
		}
	}
	
	boolean checked = false;
	
	private void check() {
		CheckVisitor checker = new CheckVisitor();
		checker.retry = false;
		for (Map.Entry<String, Pattern> p : patterns.entrySet()) {
			if (checker.patterns.containsKey(p.getKey()))
				continue;
			checker.checking = p.getKey();
			p.getValue().visit(checker);
		}
		if(checker.retry) {
			checker.defaults = checker.patterns;
			checker.patterns = new HashMap<String, Boolean>();
			for (Map.Entry<String, Pattern> p : patterns.entrySet()) {
				if (checker.patterns.containsKey(p.getKey()))
					continue;
				checker.checking = p.getKey();
				p.getValue().visit(checker);
			}
		}
		checked = true;
	}

	public Matcher matcher(String patternName, String text) {
		if(!checked)
			check();
		Matcher m = new Matcher();
		m.text = text;
		m.pattern = patterns.get(patternName);
		m.patternName = patternName;
		return m;
	}

	public static String readContents(File file) throws IOException {
		if(!file.exists()) throw new IOException("File not found "+file);
		int fileSize = (int)file.length();
		if(fileSize == 0) return "";
		char[] buf = new char[fileSize];
		FileReader fr = new FileReader(file);
		int bytesRead = fr.read(buf,0,buf.length);
		if(bytesRead != buf.length)
			throw new IOException("Could not read entire file: "+file);
		return new String(buf);
	}

	public Matcher matchFile(String fileName,String rule) throws IOException {
		String contents = readContents(new File(fileName));
		Matcher m = matcher(rule,contents);
		m.matches();
		return m;
	}
}