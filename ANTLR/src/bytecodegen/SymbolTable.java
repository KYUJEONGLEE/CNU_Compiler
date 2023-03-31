package bytecodegen;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import generated.MiniGoParser;
import generated.MiniGoParser.*;
import bytecodegen.SymbolTable.Type;
import static bytecodegen.BytecodeGenListenerHelper.*;


public class SymbolTable {
	enum Type {
		INT, INTARRAY, VOID, ERROR
	}
	
	static public class VarInfo {
		Type type; 
		int id;
		int initVal;
		
		public VarInfo(Type type,  int id, int initVal) {
			this.type = type;
			this.id = id;
			this.initVal = initVal;
		}
		public VarInfo(Type type,  int id) {
			this.type = type;
			this.id = id;
			this.initVal = 0;
		}
	}
	
	static public class FInfo {
		public String sigStr;
	}
	
	private Map<String, VarInfo> _lsymtable = new HashMap<>();	// local v.
	private Map<String, VarInfo> _gsymtable = new HashMap<>();	// global v.
	private Map<String, FInfo> _fsymtable = new HashMap<>();	// function 
	
		
	private int _globalVarID = 0;
	private int _localVarID = 0;
	private int _labelID = 0;
	private int _tempVarID = 0;
	
	SymbolTable(){
		initFunDecl();
		initFunTable();
	}
	
	void initFunDecl(){		// at each func decl
		_localVarID = 0;
		_labelID = 0;
		_tempVarID = 32;		
	}
	
	void putLocalVar(String varname, Type type){
		// local 심볼을 테이블에 추가하는 함수
		_lsymtable.put(varname, new VarInfo(type, _localVarID++));
	}
	
	// global 변수는 없다고 가정하기에 다 없애주었습니다.
	void putGlobalVar(String varname, Type type){
		// global 심볼을 테이블에 추가하는 함수
		_gsymtable.put(varname, new VarInfo(type, _globalVarID++));
	}
	
	void putLocalVarWithInitVal(String varname, Type type, int initVar){
		// local 심볼을 테이블에 추가하는 함수
		_lsymtable.put(varname, new VarInfo(type, _localVarID++, initVar));
	}
	void putGlobalVarWithInitVal(String varname, Type type, int initVar){
		// global 심볼을 테이블에 추가하는 함수
		_gsymtable.put(varname, new VarInfo(type, _globalVarID++, initVar));
	
	}
	
	void putParams(MiniGoParser.ParamsContext params) {
		// 각 parameter들을 Local 테이블에 매핑하는 메소드
		for(int i = 0; i < params.param().size(); i++) { // parameter 개수만큼 반복
			// type과 name이 필요합니다.
			String varname = getParamName(params.param(i));
			// helper에 있는 getParamName 함수로 i번째 param의 이름을 얻어옵니다.
			Type type = null; // default type = VOID로 설정
			
			// type을 체크한 후에 알맞은 type을 할당해줍니다.
			if(getTypeText(params.param(i).type_spec()) == "INT") {
				type = Type.INT;
			}
			putLocalVar(varname, type);
		}
	}
	
	private void initFunTable() {
		FInfo printlninfo = new FInfo();
		printlninfo.sigStr = "java/io/PrintStream/println(I)V";
		
		FInfo maininfo = new FInfo();
		maininfo.sigStr = "main([Ljava/lang/String;)V";
		_fsymtable.put("_print", printlninfo);
		_fsymtable.put("main", maininfo);
	}
	
	public String getFunSpecStr(String fname) {		
		// <Fill here>
		return _fsymtable.get(fname).sigStr;
	}

	public String getFunSpecStr(Fun_declContext ctx) {
		// <Fill here>	
		return _fsymtable.get(ctx.getText()).sigStr;
	}
	
	public String putFunSpecStr(Fun_declContext ctx) {
		String fname = getFunName(ctx);
		String argtype = "";	
		String rtype = "";
		String res = "";
		
		// <Fill here>	
		
		argtype = BytecodeGenListenerHelper.getParamTypesText(ctx.params());
		rtype = BytecodeGenListenerHelper.getTypeText(ctx.type_spec());
		
		res =  fname + "(" + argtype + ")" + rtype;
		
		FInfo finfo = new FInfo();
		finfo.sigStr = res;
		_fsymtable.put(fname, finfo);
		
		return res;
	}
	
	String getVarId(String name){
		// <Fill here>	
		// name을 받아와서 var_id를 반환하는 함수입니다.
		// symtable에서 name을 찾은 후 존재하면 텍스트로 id를 반환해줍니다.
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		if (lvar != null) {
			return " " + Integer.toString(lvar.id);
		}
		
		return "";
	}
	
	Type getVarType(String name){
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		if (lvar != null) {
			return lvar.type;
		}
		
		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		if (gvar != null) {
			return gvar.type;
		}
		
		return Type.ERROR;	
	}
	String newLabel() {
		return "label" + _labelID++;
	}
	
	String newTempVar() {
		String id = "";
		return id + _tempVarID--;
	}

	// global
//	public String getVarId(Var_declContext ctx) {
//		// <Fill here>	
//	}

	// local
	public String getVarId(Var_declContext ctx) {
		// <Fill here>
		return getVarId(ctx.IDENT().getText());
	}
}
