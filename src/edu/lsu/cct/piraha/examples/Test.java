package edu.lsu.cct.piraha.examples;

import edu.lsu.cct.piraha.DebugOutput;
import edu.lsu.cct.piraha.DebugVisitor;
import edu.lsu.cct.piraha.Grammar;
import edu.lsu.cct.piraha.Matcher;
import edu.lsu.cct.piraha.ParseException;
import edu.lsu.cct.piraha.Pattern;

/**
 * Just a handful of basic tests
 * @author sbrandt
 *
 */
public class Test {

	
	public static void test(String pat, String text, int matchsize) {
		Grammar g = new Grammar();
		Pattern p = g.compile("d", pat);//"a(0|1|2|3|4|5|6a|7|8|9)*x");
		String pdc = p.decompile();
		Pattern pe = null;
		DebugVisitor dv = new DebugVisitor();
		try {
			pe = g.compile("e",pdc);
		} catch (ParseException e) {
			p.visit(dv);
			DebugOutput.out.println("pat="+pat);
			DebugOutput.out.println("pdc="+pdc);
			throw new RuntimeException("parser decompile error "+pat+" != "+pdc,e);
		}
		if(!pe.equals(p)) {
			p.visit(dv);
			pe.visit(dv);
			throw new RuntimeException("decompile error "+pat+" != "+pe.decompile());
		}
		Matcher m = g.matcher(text);//"a6ax");
		//m.getPattern().diag();
		m.getPattern().visit(new DebugVisitor(DebugOutput.out));
		boolean found = m.matches();
		DebugOutput.out.print("match "+text+" using "+pat+" => "+found+" ");
		if(found) {
			DebugOutput.out.print("["+m.getBegin()+","+m.getEnd()+"]");
			m.dumpMatchesXML();
		}
		DebugOutput.out.println();
		DebugOutput.out.println();
		if(found) assert(matchsize == m.getEnd() - m.getBegin());
		else      assert(matchsize == -1);
	}
	
	static boolean assertOn = false;
	
	static public boolean turnAssertOn() {
		assertOn = true;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		assert(turnAssertOn());
		if(!assertOn)
			throw new RuntimeException("Assertions are not enabled");
		
		test("(\\[[^\\]]*)+","[",1);
		test("a(0|1|2|3|4|5|6a|7|8|9)*x","a6a72x",6);
		test("a(0|1|2|3|4|5|6a|7|8|9)*x","a6a72",-1);
		test("<{d}>|x","<<x>>",5);
		test("<{d}>|x","<<x>",-1); // this one should fail, no balancing >
		test("[a-z0-9]+","abcd0123",8);
		test("(foo|)","bar",0);
		test("(?i:foo)","bar",-1); // should fail
		test("[^\n]+","foo",3);
		test("[\u0000-\uffff]*","foo",3);
		test("(?=foo)foo","foo",3);
		test("(?!foo)foo","foo",-1);
		test("(?i:foo)","FOO",3);
		test("(?-i:foo)","foo",3);
		test("[^]+","foobar",6);
		test("[^](\\b|{d})","foo.bar",3);
		test("(?i:[a-z]+)","FOO",3);
		test("(?i:[fo]+)","FOO",3);
		test("\\t","\t",1);
		test("\t","\t",1);
		test("[a-c]+","bbca",4);
		test("(aaa|aa)aa$","aaaa",-1);
		test("a*a","aaaa",-1);
		test("\\b(?!apple\\b)[a-z]+\\b","grape",5);
		test("(?!foo)","foo",-1);
		test("(?=foo)","foo",0);
		test("(?=[^\\.]*\\.)","abcd.",0);
		
		{
			System.out.println("CHECK");
			Grammar g = new Grammar();
			g.compile("name","[a-z]+");
			g.compile("plus","{name}\\+");
			g.compile("minus", "{name}-");
			g.compile("all","{plus}|{minus}|{name}");
			Matcher mm = g.matcher("foo*");
			mm.matches();
			assert(mm.substring().equals("foo"));
		}
		
		{
			Grammar g = new Grammar();
			g.compile("int","[0-9]+");
			// Packrat allows this expression to match
			g.compile("add", "{add}\\+{add}|{int}");
			Matcher mm = g.matcher("9+20");
			mm.matches();
			assert(mm.group().substring().equals("9+20"));
		}
		
		try {
			test("a**","a",1);
			assert(false);
		} catch (ParseException e) {
			;
		}

		Grammar g;
		Matcher m;
		g = new Grammar();
		g.compile("x", "(?i:[xyz])");
		g.compile("y","(?i:{x}a\\1)");
		m = g.matcher("y", "Xax");
		assert(m.find());
		
		// Check name matches
//		g = new Grammar();
//		g.compile("name", "[a-zA-Z_][a-zA-Z0-9_]*");
//		g.compile("block", "\\{ *((decl +{name} *;|use +{$name} *;|{block}) *)*\\}");
//		g.diag(DebugOutput.out);
//		m = g.matcher("block", "{decl b;{ decl a; use a;} {{use a;}}}");
		//assert(m.matches());
		
		Grammar xml = new Grammar();
		xml.compileFile(Test.class.getResourceAsStream("xml.peg"));
		m = xml.matcher("doc","<a><b><c/></b></a>");
		if(!m.matches()) {
			System.out.println(m.near());
			assert(false);
		}
		
		// Test composabality
		g = new Grammar();
		g.importGrammar("math", Calc.makeMath());
		g.compile("vector", "{math:expr}(,{math:expr}){0,}");
		m = g.matcher("vector","1+2,(8+3)*9-4,4,9+7");
		assert(m.matches());
		
		g.compile("eol", "\n");
		g.compile("line","line");
		g.compile("line_eol","{line}{eol}");
		m = g.matcher("line_eol","line\n");
		assert(m.matches());
		
		test("[\\a-c]+","abab",4);
		test("[a-\\c]+","abab",4);
		test("[\\a-\\c]+","acb",3);
		test("[a-]+","a-a-",4);
		test("[\\a-]+","a-a-",4);
		test("[\\a\\-]+","a-a-",4);
		test("[a\\-]+","a-a-",4);
		test("[-a]+","a-a-",4);
		test("\\[a\\]","[a]",3);
		test("(\\[(\\\\[^]|[^\\]\\\\])*\\]|\\\\[^]|[^\b-\n\r ])+","xxx",3);
		test("[a-zA-Z0-9\\.\\*]+|\"[^\"]*\"|'[^']*'","\"Failed password\"",17);
		test("(b{brk}|.)*","aaabaaa",4);
		test("[^ \t\r\n\b]+","abc",3);
		test("(?i:ab(c|g)ef)","ABCEF",5);
		test("[0-67-9]+","1234",4);
		test("{OpenWhite}","   ",0);
		test("{OpenWhite}","",-1);
		test("{OpenWhite}{BodyWhite}"," \n ",1);
		test("{OpenWhite}{BodyWhite}\n{CloseWhite}x[ \t]*","  \nx",4);
		test("{OpenWhite}({BodyWhite}y\n{OpenWhite}{BodyWhite}x|{BodyWhite}y)"," y\n  z",2);
		test("{OpenWhite}({BodyWhite}y\n{OpenWhite})*"," y\n  y\n   y\n",7);
		
		g = new Grammar();
		g.compile("import", "import");
		g.compile("pat","((?!a|{import}_).)+");
		m = g.matcher("pat","foo_import_bar");
		m.find();
		System.out.println(m.substring());
		System.out.println("All tests complete");
	}
}
