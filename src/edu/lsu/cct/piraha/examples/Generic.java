package edu.lsu.cct.piraha.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import edu.lsu.cct.piraha.DebugOutput;
import edu.lsu.cct.piraha.Grammar;
import edu.lsu.cct.piraha.Matcher;

public class Generic {
	public static void usage() {
		System.err.println("usage: Generic [--perl|--python|--xml] grammar text");
		System.exit(2);
	}
	public static void main(String[] args) throws IOException {
		if(args.length < 2)
			usage();
		Grammar g = new Grammar();
		int n = 0;
		int count = 0;
		boolean checkOnly = false;
		String suffix = ".pegout";
		while(args[n].startsWith("--")) {
			if(args[n].equals("--python"))
				suffix = ".py";
			else if(args[n].equals("--perl"))
				suffix = ".pm";
			else if(args[n].equals("--xml"))
				suffix = ".xml";
			else if(args[n].equals("--check-only"))
				checkOnly = true;
			n++;
		}
		String grammarFile = args[n++];
		if(!grammarFile.endsWith(".peg"))
			usage();
		g.compileFile(new File(grammarFile));
		for(;n<args.length;n++) {
			String outputFile = args[n];
			int nn = args[n].lastIndexOf('.');
			if(nn >= 0) outputFile = outputFile.substring(0,nn);
			outputFile = outputFile+suffix;
			if(!checkOnly) {
				System.out.println("reading file: "+args[n]);
				System.out.println("writing file: "+outputFile);
				
				long inMtime = new File(args[n]).lastModified();
				long outMtime = new File(outputFile).lastModified();
				long pegMtime = new File(grammarFile).lastModified();
				
				boolean olderThanSource = inMtime < outMtime;
				boolean olderThanPeg    = pegMtime < outMtime;
				
				if(olderThanPeg && olderThanSource) {
					System.out.println(" ---> Up to date");
					continue;
				}
			}
			if(outputFile.equals(args[n])) throw new IOException("won't over-write input file "+args[n]);
			if(outputFile.equals(grammarFile)) throw new IOException("won't over-write grammar file "+grammarFile);
			Matcher m = g.matcher(Grammar.readContents(new File(args[n])));
			if(m.match(0)) {
				count++;
				if(!checkOnly) {
					FileWriter fw = new FileWriter(outputFile);
					BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter pw = new PrintWriter(bw);
					DebugOutput dout = new DebugOutput(pw);
					if(suffix.equals(".py")) {
						m.dumpMatchesPython("VAR",dout);
						dout.print("CONTENTS=\""+m.escText(m.substring())+"\"");
					} else if(suffix.equals(".pm")) {
						m.dumpMatchesPerl("$VAR",dout);
						dout.print("$CONTENTS=\""+m.escText(m.substring())+"\"");
					} else if(suffix.equals(".xml")) {
						m.dumpMatchesXML(dout,true);
					} else if(suffix.equals(".pegout")) {
						m.dumpMatches(dout);
					}
					dout.flush();
					pw.close();
				}
			} else {
				System.err.println("FAILED:  grammar=["+grammarFile+"] file=["+args[n]+"]: "+" "+m.near());
				System.exit(2);
			}
		}
		System.out.println("SUCCESS: files-checked=["+count+"] grammar=["+grammarFile+"]");
		System.exit(0);
	}
}
