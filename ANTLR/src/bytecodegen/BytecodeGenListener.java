package bytecodegen;

import java.util.Hashtable;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import generated.MiniGoBaseListener;
import generated.MiniGoParser;
import generated.MiniGoParser.*;


import static bytecodegen.BytecodeGenListenerHelper.*;
import static bytecodegen.SymbolTable.*;

public class BytecodeGenListener extends MiniGoBaseListener implements ParseTreeListener {
    ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
    SymbolTable symbolTable = new SymbolTable();

    int tab = 0;
    int label = 0;


    // program	: decl+
    @Override
    public void enterFun_decl(MiniGoParser.Fun_declContext ctx) {
        symbolTable.initFunDecl();

        String fname = getFunName(ctx);
        ParamsContext params;

        if (fname.equals("main")) {
            symbolTable.putLocalVar("args", Type.INTARRAY);
        } else {
            symbolTable.putFunSpecStr(ctx);
            params = (MiniGoParser.ParamsContext) ctx.getChild(3);
            symbolTable.putParams(params);
        }
    }


    // var_decl	:  dec_spec IDENT  type_spec
    //		| dec_spec IDENT type_spec '=' LITERAL
    //		| dec_spec IDENT '[' LITERAL ']' type_spec
    @Override
    public void enterVar_decl(MiniGoParser.Var_declContext ctx) {
        String varName = ctx.IDENT().getText();

        if (isArrayDecl(ctx)) {
            symbolTable.putGlobalVar(varName, Type.INTARRAY);
        }
        else if (isDeclWithInit(ctx)) {
            // Fill here
        	symbolTable.putGlobalVarWithInitVal(varName, Type.INT, Integer.parseInt(ctx.LITERAL().getText()));
        }
        else  { // simple decl
            symbolTable.putGlobalVar(varName, Type.INT);
        }
    }
    
    // local_decl	: var_decl	; 문법에 따라서 var_decl로 put
    @Override
    public void exitLocal_decl(MiniGoParser.Local_declContext ctx) {
    	String decl = "";
		decl += newTexts.get(ctx.var_decl());
		newTexts.put(ctx, decl);
    }
    
    @Override
    public void exitProgram(MiniGoParser.ProgramContext ctx) {
        String classProlog = getFunProlog();

        String fun_decl = "", var_decl = "";

        for(int i = 0; i < ctx.getChildCount(); i++) {
            if(isFunDecl(ctx, i))
                fun_decl += newTexts.get(ctx.decl(i));
            else
                var_decl += newTexts.get(ctx.decl(i));
        }

        newTexts.put(ctx, classProlog + var_decl + fun_decl);

        System.out.println(newTexts.get(ctx));
    }


    // decl	: var_decl | fun_decl
    @Override
    public void exitDecl(MiniGoParser.DeclContext ctx) {
        String decl = "";
        if(ctx.getChildCount() == 1)
        {
            if(ctx.var_decl() != null)				//var_decl
                decl += newTexts.get(ctx.var_decl());
            else							//fun_decl
                decl += newTexts.get(ctx.fun_decl());
        }
        newTexts.put(ctx, decl);
    }

    // stmt	: expr_stmt | compound_stmt | if_stmt | for_stmt | return_stmt
    @Override
    public void exitStmt(MiniGoParser.StmtContext ctx) {
        String stmt = "";
        if(ctx.getChildCount() > 0)
        {	
        	// stmt 생성규칙에 따르면 총 5가지 경우가 존재합니다.
        	// case를 나누어서 각각의 stmt 상태에 맞게 조건문을 설정하고 추가해주었습니다.
        	// null으로 상태를 체크할수 있습니다.
            if(ctx.expr_stmt() != null)				// expr_stmt
                stmt += newTexts.get(ctx.expr_stmt());
            else if(ctx.compound_stmt() != null)	// compound_stmt
                stmt += newTexts.get(ctx.compound_stmt());
            else if(ctx.if_stmt() != null) {				// if_stmt
				stmt += newTexts.get(ctx.if_stmt());
			}else if(ctx.for_stmt() != null) {				// for_stmt
				stmt += newTexts.get(ctx.for_stmt());
			} else if(ctx.return_stmt() != null) {			// return_stmt
				stmt += newTexts.get(ctx.return_stmt());
			}			
        }
        newTexts.put(ctx, stmt);
    }

    // expr_stmt	: expr ';'
    @Override
    public void exitExpr_stmt(MiniGoParser.Expr_stmtContext ctx) {
        String stmt = "";
        if(ctx.getChildCount() == 2)
        {
            stmt += newTexts.get(ctx.expr());	// expr
        }
        newTexts.put(ctx, stmt);
    }




    @Override
    // fun_decl			: FUNC IDENT '(' params ')' type_spec compound_stmt ;  
    public void exitFun_decl(MiniGoParser.Fun_declContext ctx) {
        // <(2) Fill here!>
    	String stmt = "";
    	String fcompound = newTexts.get(ctx.compound_stmt());
    	
    	String fname = getFunName(ctx);
    	String fheader = funcHeader(ctx, fname);
    	// header + compound stmt 
    	// ctx return 타입이 void인 경우 return 추가
    	// helper의 getTypeText 함수 활용
    	if(getTypeText(ctx.type_spec()) == "VOID"){
    		stmt += "return\n";
    	}
    	stmt += fheader + fcompound + ".end method\n";
    	
    	newTexts.put(ctx, stmt);
    }	


    private String funcHeader(MiniGoParser.Fun_declContext ctx, String fname) {
        return ".method public static " + symbolTable.getFunSpecStr(fname) + "\n"
                + "\t" + ".limit stack " 	+ getStackSize(ctx) + "\n"
                + "\t" + ".limit locals " 	+ getLocalVarSize(ctx) + "\n";

    }



    @Override
    public void exitVar_decl(MiniGoParser.Var_declContext ctx) {
        String varName = ctx.IDENT().getText();
        String varDecl = "";

        if (isDeclWithInit(ctx)) {
            varDecl += "putfield " + varName + "\n";
            // v. initialization => Later! skip now..: 
        }
        newTexts.put(ctx, varDecl);
    }

    // for문 bytecode 변환
    // 추가한 exitFor_stmt 함수
    // 아래의 exitIf_stmt 함수를 참고하여 작성하였습니다.
    // for_stmt			:  FOR expr stmt
    public void exitFor_stmt(MiniGoParser.For_stmtContext ctx) {
        // <(3) Fill here>
    	String stmt = "";
    	String condExpr= newTexts.get(ctx.expr()); // expr
        String thenStmt = newTexts.get(ctx.stmt()); // stmt
        
        String loopstart = symbolTable.newLabel(); // loopstart label
    	String loopend   = symbolTable.newLabel(); // loopend label
    	
    	// loop 시작지점을 추가
    	// ifeq = false 면 loop를 끝내는 label(loopend)로 이동
    	// 만약 아니라면 조건문장을 읽게한 후 다시 start로 돌아가게 합니다.
    	stmt += loopstart + "\n"
    			+ condExpr + "\n" 
    			+ "ifeq" + loopend + "\n"
    			+ thenStmt + "goto" + loopstart + "\n"
    			+ loopend + ":" + "\n";
    	
    	newTexts.put(ctx, stmt);
    }

    // compound_stmt	: '{' local_decl* stmt* '}'
    @Override
    public void exitCompound_stmt(MiniGoParser.Compound_stmtContext ctx) {
        // <(3) Fill here>
    	// 복합문 bytecode 변환
    	String stmt = "";
    	
    	// local_decl * = local_decl 여러번 반복
    	// stmt * = stmt 여러번 반복의 구조입니다.
    	// 각 size 만큼 반복문을 돌면서 stmt에 추가해줍니다.
    	for (int i = 0 ; i < ctx.local_decl().size(); i++) {
    		stmt += newTexts.get(ctx.local_decl(i));
    	}
    	for (int i = 0; i < ctx.stmt().size(); i++) {
    		stmt += newTexts.get(ctx.stmt(i));
    	}
    	newTexts.put(ctx, stmt);
    }

    // if_stmt		:  IF  expr  stmt
    //		| IF  expr  stmt ELSE stmt   ;
    @Override
    public void exitIf_stmt(MiniGoParser.If_stmtContext ctx) {
        String stmt = "";
        String condExpr= newTexts.get(ctx.expr());
        String thenStmt = newTexts.get(ctx.stmt(0));

        String lend = symbolTable.newLabel();
        String lelse = symbolTable.newLabel();


        if(noElse(ctx)) {
            stmt += condExpr + "\n"
                    + "ifeq " + lend + "\n"
                    + thenStmt + "\n"
                    + lend + ":"  + "\n";
        }
        else {
            String elseStmt = newTexts.get(ctx.stmt(1));
            stmt += condExpr + "\n"
                    + "ifeq " + lelse + "\n"
                    + thenStmt + "\n"
                    + "goto " + lend + "\n"
                    + lelse + ": " + elseStmt + "\n"
                    + lend + ":"  + "\n";
        }

        newTexts.put(ctx, stmt);
    }


    // return_stmt	: RETURN
    //		| RETURN expr
    //		| RETURN expr ',' expr	 ;
    @Override
    public void exitReturn_stmt(MiniGoParser.Return_stmtContext ctx) {
        // <(4) Fill here>
    	String stmt = "";
    	// 3가지 상황을 count로 조건을 나누어서 입력해주었습니다.
    	// 정수값을 리턴받는 경우에 ireturn 사용
    	if(ctx.getChildCount() == 5) {
    		stmt += newTexts.get(ctx.expr(1)) + "\n" + newTexts.get(ctx.expr(0)) + "ireturn\n";
    	}else if(ctx.getChildCount() == 2){
    		stmt += newTexts.get(ctx.expr(0)) + "ireturn\n";
    	}else if(ctx.getChildCount() == 1) {
    		stmt += "return\n";
    	}
    	
    	newTexts.put(ctx, stmt);
    }


    // warning! Too many holes. You should check the rules rather than use them as is.
    @Override
    public void exitExpr(MiniGoParser.ExprContext ctx) {
        String expr = "";

        if(ctx.getChildCount() <= 0) {
            newTexts.put(ctx, "");
            return;
        }

        if(ctx.getChildCount() == 1) { // IDENT | LITERAL
            if(ctx.IDENT() != null) {
                String idName = ctx.IDENT().getText();
                if(symbolTable.getVarType(idName) == Type.INT) {
                    expr += "iload_" + symbolTable.getVarId(idName) + " \n";
                }
                //else	// Type int array => Later! skip now..
                //	expr += "           lda " + symbolTable.get(ctx.IDENT().getText()).value + " \n";
            } else if (ctx.LITERAL() != null) {
                String literalStr = ctx.LITERAL().getText();
                expr += "ldc " + literalStr + " \n";
            }
        } else if(ctx.getChildCount() == 2) { // UnaryOperation
            expr = handleUnaryExpr(ctx, newTexts.get(ctx) + expr);
        } else if(ctx.getChildCount() == 3) {
            if(ctx.getChild(0).getText().equals("(")) { 		// '(' expr ')'
                expr = newTexts.get(ctx.expr(0));

            } else if(ctx.getChild(1).getText().equals("=")) { 	// IDENT '=' expr
                expr = newTexts.get(ctx.expr(0))
                        + "istore_" + symbolTable.getVarId(ctx.IDENT().getText()) + " \n";

            } else { 											// binary operation
                expr = handleBinExpr(ctx, expr);

            }
        }
        // IDENT '(' args ')' |  IDENT '[' expr ']'
        else if(ctx.getChildCount() == 4) {
            if(ctx.args() != null){		// function calls
                expr = handleFunCall(ctx, expr);
            } else { // expr
                // Arrays: TODO  
            }
        }
        // IDENT '[' expr ']' '=' expr
        else { // Arrays: TODO			*/
        }
        newTexts.put(ctx, expr);
    }


    private String handleUnaryExpr(MiniGoParser.ExprContext ctx, String expr) {
        String l1 = symbolTable.newLabel();
        String l2 = symbolTable.newLabel();
        String lend = symbolTable.newLabel();

        expr += newTexts.get(ctx.expr(0));
        switch(ctx.getChild(0).getText()) {
            case "-":
                expr += "           ineg \n"; break;
            case "--":
                expr += "ldc 1" + "\n"
                        + "isub" + "\n";
                break;
            case "++":
                expr += "ldc 1" + "\n"
                        + "iadd" + "\n";
                break;
            case "!":
                expr += "ifeq " + l2 + "\n"
                        + l1 + ": " + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": " + "\n";
                break;
        }
        return expr;
    }


    private String handleBinExpr(MiniGoParser.ExprContext ctx, String expr) {
        String l2 = symbolTable.newLabel();
        String lend = symbolTable.newLabel();

        expr += newTexts.get(ctx.expr(0));
        expr += newTexts.get(ctx.expr(1));

        switch (ctx.getChild(1).getText()) {
            case "*":
                expr += "imul \n"; break;
            case "/":
                expr += "idiv \n"; break;
            case "%":
                expr += "irem \n"; break;
            case "+":		// expr(0) expr(1) iadd
                expr += "iadd \n"; break;
            case "-":
                expr += "isub \n"; break;

            case "==":
                expr += "isub " + "\n"
                        + "ifeq "+ l2 + "\n"
                        + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": " + "\n";
                break;
            case "!=":
                expr += "isub " + "\n"
                        + "ifne " + l2 + "\n"
                        + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": " + "\n";
                break;
            case "<=":
                // <(5) Fill here>
            	// <= 연산 같은 경우에는 ifle(value가 0보다 작거나 같으면 이동)을 사용합니다.
            	expr += "isub " + "\n"
                        + "ifle " + l2 + "\n"
                        + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": " + "\n";
                break;
            case "<":
                // <(6) Fill here>
            	// < 연산의 경우네는 iflt(value가 0보다 작으면 이동)을 사용합니다.
            	expr += "isub " + "\n"
                        + "iflt " + l2 + "\n"
                        + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": " + "\n";
                break;

            case ">=": // ifge 사용
            	expr += "isub " + "\n"
                        + "ifge " + l2 + "\n"
                        + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": " + "\n";
                // <(7) Fill here>

                break;

            case ">": // ifgt 사용
            	expr += "isub " + "\n"
                        + "ifgt" + l2 + "\n"
                        + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": " + "\n";
                // <(8) Fill here>
                break;

            case "and":
                expr +=  "ifne "+ lend + "\n"
                        + "pop" + "\n" + "ldc 0" + "\n"
                        + lend + ": " + "\n"; break;
            case "or":
            	// 
                // <(9) Fill here>
            	expr +=  "ifne "+ lend + "\n"
                        + "pop" + "\n" + "ldc 1" + "\n"
                        + lend + ": " + "\n"; break;

        }
        return expr;
    }
    private String handleFunCall(MiniGoParser.ExprContext ctx, String expr) {
        String fname = getFunName(ctx);

        if (fname.equals("_print")) {		// System.out.println	
            expr = "getstatic java/lang/System/out Ljava/io/PrintStream; " + "\n"
                    + newTexts.get(ctx.args())
                    + "invokevirtual " + symbolTable.getFunSpecStr("_print") + "\n";
        } else {
            expr = newTexts.get(ctx.args())
                    + "invokestatic " + getCurrentClassName()+ "/" + symbolTable.getFunSpecStr(fname) + "\n";
        }

        return expr;

    }

    // args	: expr (',' expr)* | ;
    @Override
    public void exitArgs(MiniGoParser.ArgsContext ctx) {

        String argsStr = "\n";

        for (int i=0; i < ctx.expr().size() ; i++) {
            argsStr += newTexts.get(ctx.expr(i)) ;
        }
        newTexts.put(ctx, argsStr);
    }

}
