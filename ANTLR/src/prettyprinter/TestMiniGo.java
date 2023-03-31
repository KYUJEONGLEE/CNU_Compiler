package prettyprinter;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import generated.*;


public class TestMiniGo {
	public static void main(String[] args) throws Exception {
		CharStream codeCharStream = CharStreams.fromFileName("test1.go");
		MiniGoLexer lexer = new MiniGoLexer(codeCharStream);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		MiniGoParser parser = new MiniGoParser(tokens);
		ParseTree tree = parser.program();
		
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk((ParseTreeListener) new MiniGoPrintListener(), tree);
	}
}