package abacus.clue.commands;


public class AbacusClient {
	public static void main(String[] args) throws Exception {
	  if (args.length < 1){
      System.out.println("usage: <index location> <command> <command args>");
      System.exit(1);
    }
	  
	  String idxLocation = args[0];
		AbacusClueApplication app = new AbacusClueApplication(idxLocation, true);
		
		app.run();
	}
}
