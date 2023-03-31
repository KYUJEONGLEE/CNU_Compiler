package prettyprinter;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import java.io.File;
import java.io.FileWriter; 
import java.io.IOException;
import generated.*;

public class MiniGoPrintListener extends MiniGoBaseListener {
	ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
	int fun_tab_count = 0;
	int if_tab_count = 0;
	int compound_tab_count = 0;
	
	public String tab_iter(int count) {
		// ....을 찍기 위한 함수
		String tab = "....";
		for(int i = 0; i < count; i++) {
			tab += tab;
		}
		return tab;
	}
	
	@Override
	public void exitProgram(MiniGoParser.ProgramContext ctx) {
		String program = "";

		for (int i = 0; i < ctx.getChildCount(); i++) {
			newTexts.put(ctx, ctx.decl(i).getText());
			program += newTexts.get(ctx.getChild(i));
		}
		
		System.out.println(program);
		File file = new File(String.format("[HW3]201602037.c"));
		
		try {
			FileWriter fw = new FileWriter(file);
			fw.write(program);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override 
	public void exitDecl(MiniGoParser.DeclContext ctx) {
		/*
		 * ParseTree에서 decl를 만났을때, 처리해주는 함수입니다. grammar를 살펴보면 decl은 2가지 상태를 가지고 있습니다.
		 * var_decl, fun_decl 조건문을 사용하여 원소를 구분하여 newTexts에 어떤값을 put 할 것 인지 정해줍니다.
		 * getChild(0)을 확인하여 보니 입력한 파일의 text가 저장되어 있었습니다. 조건문의 조건에는 equals를 사용하여
		 * 해당 객체가 특정한 클래스에 속하는지 아닌지를 구분하고 newTexts에 put 하여줍니다.
		 */
		if (ctx.getChild(0).equals(ctx.var_decl())) {
			newTexts.put(ctx, newTexts.get(ctx.var_decl()));
		} else if (ctx.getChild(0).equals(ctx.fun_decl())) {
			newTexts.put(ctx, newTexts.get(ctx.fun_decl()));
		}
		
	}
	
	@Override 
	public void exitVar_decl(MiniGoParser.Var_declContext ctx) { 
		// val_decl를 처리하는 함수입니다. 
		// val_dec1은 grammar에서 총 3가지 상태를 가지고있습니다. 
		// 직관적으로 grammar를 child의 개수로 나누어서 비교하였습니다.
		String type_spec = newTexts.get(ctx.type_spec());
		String ident = ctx.getChild(1).getText();
		String Var_decl = null;
		
		if (ctx.getChildCount() == 3) { 
			Var_decl = ctx.dec_spec().getText() + " " + ident + "" + type_spec + "\n";
		} else if (ctx.getChildCount() == 5) {
			Var_decl = ctx.dec_spec().getText() + " " + ident + "" + type_spec + "= " 
					+ ctx.LITERAL().getText() + "\n";
		} else if (ctx.getChildCount() == 6) {
			Var_decl = ctx.dec_spec().getText() + " " + ident + " [ " + ctx.LITERAL().getText() + " ] "
					+ type_spec + "\n";
		}
		newTexts.put(ctx, Var_decl);
	}
	
	@Override 
	public void exitLocal_decl(MiniGoParser.Local_declContext ctx) {
		String Loc_decl = null;
		Loc_decl = tab_iter(fun_tab_count) + newTexts.get(ctx.var_decl());
		newTexts.put(ctx, Loc_decl);
	}
	@Override
	public void exitDec_spec(MiniGoParser.Dec_specContext ctx) {
		// dec_spec 은 VAR를 반환합니다.
		newTexts.put(ctx, ctx.VAR().getText());
	}
	
	@Override 
	public void exitType_spec(MiniGoParser.Type_specContext ctx) { 
		// Type_spec 을 살펴보면 3가지 child가 존재합니다.
		// 각 child에 맞는 조건물을 설정하여 put 해주었습니다.
		
		if(ctx.getChild(0) == null) {
			newTexts.put(ctx, " ");
		}else if(ctx.getChild(0).equals(ctx.INT())) {
			newTexts.put(ctx, ctx.INT().getText());
		}else if(ctx.getChild(0).equals(ctx.LITERAL())) {
			newTexts.put(ctx, ctx.LITERAL().getText());
		}
	}
	
	@Override 
	public void enterFun_decl(@NotNull MiniGoParser.Fun_declContext ctx) { 
		//fun_tab_count ++; // func에 들어갈때 tab count를 늘려줍니다.
	}
	
	@Override
	public void exitFun_decl(MiniGoParser.Fun_declContext ctx) { 
		// Fun_decl 는 하나의 child 만 존재하고 알아보기 쉽게 변수를 선언하여 저장하고 put 해주었습니다.
		String type_spec = newTexts.get(ctx.type_spec());
		String ident = ctx.getChild(1).getText();
		String func = ctx.FUNC().getText();
		String params = newTexts.get(ctx.params());
		String compound_stmt = newTexts.get(ctx.compound_stmt());
		
		String Fun_decl = func + " " + ident + "(" + params
				+ ") " + type_spec + "\n" + compound_stmt;
		
		newTexts.put(ctx, Fun_decl);
		fun_tab_count--; // func을 나갈떄 tab count를 감소시켜 tab 횟수를 줄입니다.
		
	}
	
	@Override 
	public void exitParams(MiniGoParser.ParamsContext ctx) { 
		// getChild(0) = null | param
		// getChild(1) = null | (',' param)
		// ... 
		// getCHild(n) = null | (',' param)
		
		// Params 는 2개의 child가 존재하고 하나는 null, 다른 하나는 param 이거나 (',' param)이 0~n번까지 나옵니다.
		// n번까지 나올수 있기떄문에 Child를 count하고 count 되는 수만큼 (',' param)을 put 해줍니다.
		String param = newTexts.get(ctx.param(0));
		// 기본적으로 child가 null 이 아니면 한번 이상 param이 등장하기 때문에 count가 늘어날때마다 += 시켜줍니다.
		if(ctx.getChild(0) == null) {
			newTexts.put(ctx, "");
		}else {
			if(ctx.getChildCount() > 1) {
				for(int i = 1; i < ctx.getChildCount(); i++) {
					param += ", " + newTexts.get(ctx.param(i));
				}
			}
			newTexts.put(ctx, param);
		}
	}
	
	@Override
	public void exitParam(MiniGoParser.ParamContext ctx) {
		// Param : IDENT | IDENT type_spec
		if(ctx.getChildCount() == 1) {
			newTexts.put(ctx, ctx.getChild(0).getText());
		}else if(ctx.getChildCount() == 2){
			newTexts.put(ctx, ctx.getChild(0).getText() + " " + newTexts.get(ctx.type_spec()));
		}
	}
	
	@Override 
	public void exitStmt(@NotNull MiniGoParser.StmtContext ctx) {
		// tab_iter 함수로 stmt마다 tab을 이 함수에서 해주었습니다.
		// 제대로 동작하지 않아 tab_iter의 매개변수를 임의로 조정해주었습니다.
		if(ctx.getChild(0) instanceof MiniGoParser.Expr_stmtContext) {
			newTexts.put(ctx, tab_iter(fun_tab_count+1) + newTexts.get(ctx.expr_stmt()));
		}else if(ctx.getChild(0) instanceof MiniGoParser.Compound_stmtContext) {
			newTexts.put(ctx, tab_iter(compound_tab_count-1) + newTexts.get(ctx.compound_stmt()));
		}else if(ctx.getChild(0) instanceof MiniGoParser.Return_stmtContext) {
			newTexts.put(ctx,  newTexts.get(ctx.return_stmt()));
		}else if(ctx.getChild(0) instanceof MiniGoParser.If_stmtContext) {
			newTexts.put(ctx, tab_iter(if_tab_count) + newTexts.get(ctx.if_stmt()));
		}else if (ctx.getChild(0) instanceof MiniGoParser.For_stmtContext) {
			newTexts.put(ctx, newTexts.get(ctx.for_stmt()));
		}
	
		
	}
	
	@Override 
	public void exitExpr_stmt(@NotNull MiniGoParser.Expr_stmtContext ctx) { 
		newTexts.put(ctx, newTexts.get(ctx.expr()));
	}
	
	
	@Override 
	public void exitExpr(@NotNull MiniGoParser.ExprContext ctx) {
		String s1 = null, s2 = null, op = null;
		String expr = null, args = null;
		String expr1 = null;
		
		
		if (ctx.getChildCount() == 1) { // LITERAL | IDENT 일때
			newTexts.put(ctx, ctx.getChild(0).getText());
		}
		
		else if(ctx.getChildCount() == 2) { // op expr 일때
			s1 = newTexts.get(ctx.expr(0));
			// 각각의 연산자들을 처리
			if(ctx.getChild(0).equals("-")){
				newTexts.put(ctx, "-" + s1);
			}else if(ctx.getChild(0).equals("+")){
				newTexts.put(ctx, "+" + s1);
			}else if(ctx.getChild(0).equals("--")){
				newTexts.put(ctx, "--" + s1);
			}else if(ctx.getChild(0).equals("++")){
				newTexts.put(ctx, "++" + s1);
			}else if(ctx.getChild(0).equals("!")){
				newTexts.put(ctx, "!" + s1);
			}
		}
		
		else if(ctx.getChildCount() == 3) { 
			if(ctx.getChild(1) instanceof MiniGoParser.ExprContext) { 
				// '(' expr ')' 일 때 child(1)의 값으로 비교합니다.
				s1 = ctx.getChild(0).getText();
				s2 = ctx.getChild(2).getText();
				expr = newTexts.get(ctx.expr(0));
				
				newTexts.put(ctx, s1 + expr + s2);
			}else if(ctx.getChild(0) == ctx.IDENT()) { 
				// IDENT '=' expr 일 때 child(0)이 indent이면 조건문에 들어갑니다.
				s1 = ctx.getChild(0).getText();
				s2 = ctx.getChild(1).getText();
				expr = ctx.expr(0).getText();
				//System.out.print(s1 + ","+s2+","+ ctx.expr(0).getText()+"END");
				newTexts.put(ctx, s1 + " " + s2 + " " + expr);
			}else{
				// expr "+" expr 일 때
				s1 = newTexts.get(ctx.expr(0));
				s2 = newTexts.get(ctx.expr(1));
				op = ctx.getChild(1).getText();
				//System.out.println(s1 + ","+s2+","+ op +" END");
				newTexts.put(ctx, s1 + " " + op + " " + s2);
			}
		}
		
		else if (ctx.getChildCount() == 4) {
			// IDENT '[' expr ']' | IDENT '(' args ')' 의 부분
			// getChild(1)에 들어있는 '(' | '[' 값을 비교하여 조건에 맞는 put 식으로 작성했습니다.
			expr = newTexts.get(ctx.expr(0));
			args = newTexts.get(ctx.args());
			s1 = ctx.getChild(0).getText();
			
			if (ctx.getChild(1).getText().equals("[")) {
				newTexts.put(ctx, s1 + " [ " + expr + " ] ");
			} else {
				newTexts.put(ctx, s1 + " (" + args + ") ");
			}
		}
		else if(ctx.getChildCount() == 6) {
			// IDENT '[' expr ']' '=' expr 일 떄
			expr = newTexts.get(ctx.expr(0));
			expr1= newTexts.get(ctx.expr(1));
			
			s1 = ctx.getChild(0).getText();
			
			newTexts.put(ctx, s1 + "[" + expr + "]" + " = " + expr1);
		}
	}
	
	@Override
	public void enterCompound_stmt(MiniGoParser.Compound_stmtContext ctx) 
	{	
		//Compound_stmt 에 들어갈때마다 count ++ 를 시켜주어 tab_iter에 들어가는 변수를 조정합니다.
		compound_tab_count++;
	}
	
	@Override 
	public void exitCompound_stmt(MiniGoParser.Compound_stmtContext ctx) {
		// getChild(0) = '{'
		// getChild(1) = local_decl | stmt | null
		// ... 
		// getCHild(n) = local_decl | stmt | null
		// getChild(n+1) = '}'
		//int total_count = if_tab_count + compound_tab_count + fun_tab_count;
		
		String start_symbol = ctx.getChild(0).getText()+"\n";
		String end_symbol = ctx.getChild(ctx.getChildCount()-1).getText();
		String local_decl = "";
		String stmt = "";
		
		String result = "";

		for(int i = 0; i < ctx.local_decl().size(); i++) {
			result += newTexts.get(ctx.local_decl(i));
			
		}
		
		for(int i = 0; i < ctx.stmt().size(); i++) {
			result += newTexts.get(ctx.stmt(i));
		}
		
		newTexts.put(ctx, start_symbol + result +"\n"+ tab_iter(if_tab_count) + end_symbol);
		compound_tab_count --;
		
	}
	
	@Override public void enterIf_stmt(MiniGoParser.If_stmtContext ctx) 
	{
		if_tab_count++;
	}
	
	@Override 
	public void exitIf_stmt(@NotNull MiniGoParser.If_stmtContext ctx) {
		// IF expr stmt | IF expr stmt ELSE stmt 의 부분
		String s1 = "" , s2 = "", result = "";
		String expr = "", stmt = "", stmt1 = "";
		
		s1 = ctx.getChild(0).getText();
		
		expr = newTexts.get(ctx.expr());
		//System.out.print("expr :" + ctx.getChild(1).getText());
		stmt = newTexts.get(ctx.stmt(0));
		stmt1= newTexts.get(ctx.stmt(1));
		
		if (ctx.getChildCount() == 3) {
			result = s1 + ctx.getChild(1).getText() +"\n"+ stmt;
			//System.out.println("if:"+stmt);
			newTexts.put(ctx,result);
		}
	
		else if(ctx.getChildCount() == 5) {
			s2 = ctx.getChild(3).getText();
			result = s1 + "(" + expr + ")" + stmt + s2 + stmt1;
			newTexts.put(ctx,result);
		}
		if_tab_count--;
	}
	
	@Override 
	public void exitFor_stmt(@NotNull MiniGoParser.For_stmtContext ctx) { 
		
	}
	
	
	@Override 
	public void exitReturn_stmt(@NotNull MiniGoParser.Return_stmtContext ctx) {
//		if(ctx.getChildCount() == 1) {
//			newTexts.put(ctx, ctx.RETURN().getText());
//		}else if(ctx.getChildCount() == 2) {
//			//newTexts.put(ctx, ctx.RETURN().getText() + " " + newTexts.get(ctx.expr()));
//		}else if(ctx.getChildCount() == 4){
//			newTexts.put(ctx, ctx.RETURN().getText() + " " + ctx.getChild(1).getText());
//		}
		
		/*
		 * for(int i = 0; i < ctx.getChildCount(); i++) {
		 * 
		 * System.out.println(ctx.getChild(i).getText()); }
		 */
		 
	}

	
}