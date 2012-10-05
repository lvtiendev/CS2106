package project1;

import java.util.*;

/**
 * PCB class
 * @author Tien
 */
class PCB {
	public String name;
	public int PID;
	// List of resource this PCB holds
	public Vector<RCB> resource;
	// Parent of this PCB in creation tree
	public PCB parent;
	// List of children of this PCB in creation tree
	public Vector<PCB> children;
	public int priority;
	public int statusType;  // 0 is block, 1 is ready, 2 is running
	// The RCB in which this PCB is blocked
	public RCB statusList;
	
	// Constructor
	public PCB(String name, int priority){
		this.name = name;
		PID = Manager.getNewPID();
		resource = new Vector<RCB>();
		parent = null;
		children = new Vector<PCB>();
		this.priority = priority;
		statusType = 1;
		statusList = null;
	}
}

/**
 * RCB class
 * @author Tien
 */
class RCB {
	public String name;
	public int RID;
	// number of remaining units of this resource
	public int remain;
	// list of PCB blocked on this RCB
	public ArrayList<PCB> waitList;
	
	public RCB(String name, int maxNum){
		this.name = name;
		RID = Manager.getNewRID();
		remain = maxNum;
		waitList = new ArrayList<PCB>();
	}
}

public class Manager {
	final static String PROCESS_RUNNING = "%s is running";
	final static String QUIT_STRING = "process terminated";
	final static String INVALID_COMMAND_STRING = "error";
	
	enum COMMAND_TYPE {
			CR, DE, QUIT, TO, REQ, REL, INIT, RIO, IOC, INVALID, BLANK
	};
	
	static String userCommand;
	 
	// check if can terminate the program
	static boolean isWorking = true;
	
	static int numPID = -1;
	static int numRID = 0;
	
	static PCB currentProcess;
	
	// Ready list
	static Vector<PCB> RL = new Vector<PCB>();
	
	// Block list
	static Vector<PCB> BL = new Vector<PCB>();
	
	// Map name - PCB
	static TreeMap<String, PCB> PCBMap = new TreeMap<String, PCB>();
	
	// Map name - RCB
	static TreeMap<String, RCB> RCBMap = new TreeMap<String, RCB>();
	
	// Represent IO resource
	static RCB IO = new RCB("IO", 0);
	
	static int getNewPID(){
		return ++numPID;
	}
	
	static int getNewRID() {
		return ++numRID;
	}
	
	/**
	 * scheduler method
	 * find next process to run
	 */
	static void scheduler(){
		int maxPriority = -1;
		PCB nextPCB = null;
		
		// Change the statusType of currentProcess to "ready"
		if (currentProcess.statusType == 2){
			currentProcess.statusType = 1;
		} 
		
		// Find the process in ReadyList with highest priority
		for (PCB pcb : RL){
			if ((pcb.statusType == 1) && (pcb.priority > maxPriority)){
				maxPriority = pcb.priority;
				nextPCB = pcb;
			}
		}
		
		// Found new process to run
		currentProcess = nextPCB;
		currentProcess.statusType = 2;
	}
	
	/**
	 * method to create & init new process
	 * @throws Exception
	 */
	static void create() throws Exception{
		// Handle the command
		String[] params = splitParameters(userCommand);
		if (params.length < 3) throw new Exception("error");
		
		// Get the name & priority of new process
		String name = params[1];
		int priority = Integer.parseInt(params[2]);
		if (priority <= 0) throw new Exception("error");
		if (priority > 2) throw new Exception("error");
		
		// create new PCB
		PCB newPCB = new PCB(name, priority);
		newPCB.parent = currentProcess;
		currentProcess.children.add(newPCB);
		RL.add(newPCB);
		PCBMap.put(name, newPCB);
		
		scheduler();
	}
	
	/**
	 * method to remove a RCB from a PCB, used in release() and destroy()
	 */
	static void freeResource(PCB pcb, RCB rcb){
		// increase the number of remaining units of RCB
		int unitTaking = 1;
		rcb.remain += unitTaking;
		
		if (rcb.waitList.size() != 0){
			// Find next PCB blocked on this RCB
			PCB nextPCB = rcb.waitList.remove(0);
			// Add this PCB to the next PCB
			nextPCB.resource.add(rcb);
			// Unblock the PCB
			RL.add(nextPCB);
			nextPCB.statusType = 1;
			BL.remove(nextPCB);
			rcb.remain -= unitTaking;
		}
	}
	
	/**
	 * method to destroy a PCB and its children from creation tree
	 * @param parentPCB
	 * @throws Exception
	 */
	static void killTree(PCB parentPCB)throws Exception{
		 // for each of its children, call killTree
		 for (PCB childrenPCB : parentPCB.children){
			 killTree(childrenPCB);
		 }
		 
		 // remove all resources of this PCB
		 for (RCB rcb : parentPCB.resource){
			 freeResource(parentPCB, rcb);
		 }
		 parentPCB.resource.removeAllElements();
		 
		 
		 // if the PCB is blocked, remove it from waitList of blocking RCB
		 if (parentPCB.statusType == 0){
			 RCB rcb = parentPCB.statusList;
			 rcb.waitList.remove(parentPCB);
		 }
		 
		 // remove the PCB from ReadyList, BlockList, Map
		 parentPCB.priority = -1;
		 RL.remove(parentPCB);
		 BL.remove(parentPCB);
		 PCBMap.remove(parentPCB.name);
	}
	
	/**
	 * method to destroy a specified PCB
	 * @throws Exception
	 */
	static void destroy() throws Exception{
		// Handle the command
		String[] params = splitParameters(userCommand);
		if (params.length < 2) throw new Exception("invalid command: must provide process name");
		
		// Get process's name
		String name = params[1];
		PCB requestedPCB = PCBMap.get(name);
		if (requestedPCB == null) throw new Exception("PCB not exist");
		
		// call kill tree method
		killTree(requestedPCB);
		
		scheduler();
	}
					
	/**
	 * method to generate initial state of the program
	 */
	static void init(){
		numPID = -1;
		numRID = 0;
		 
		// Reset the RCB set
		RCBMap.clear();
		RCBMap.put("R1", new RCB("R1", 1));
		RCBMap.put("R2", new RCB("R2", 1));
		RCBMap.put("R3", new RCB("R3", 1));
		RCBMap.put("R4", new RCB("R4", 1));
		
		// Reset the PCB set
		PCBMap.clear();
		
		// Generate "unit" process with priority = 0
		currentProcess = new PCB("Init", 0);
		currentProcess.statusType = 2;
		PCBMap.put("Init", currentProcess);
		
		// reset ready list, add current process to ready list
		RL.removeAllElements();
		RL.add(currentProcess);
		
		// reset block list
		BL.removeAllElements();
		
		printResult();
	}
	
	/**
	 * method to request a RCB from a PCB
	 * @throws Exception
	 */
	static void request()throws Exception{
		// Handle the command
		int unitTaking = 1;
		String[] params = splitParameters(userCommand);
		if (params.length < 2) throw new Exception("error");
		
		// get RCB's name
		String name = params[1];
		RCB requestedRCB = RCBMap.get(name);
		if (requestedRCB == null)throw new Exception("error");
		
		// check if the requtestedRCB has enough unit
		if (unitTaking <= requestedRCB.remain){ 
			requestedRCB.remain -= unitTaking;
			currentProcess.resource.add(requestedRCB);
		}
		else{
			// the PCB is blocked
			RL.remove(currentProcess);
			requestedRCB.waitList.add(currentProcess);
			currentProcess.statusType = 0;
			currentProcess.statusList = requestedRCB;
			BL.add(currentProcess);
		}
		scheduler();
	}
	
	/**
	 * method to release a RCB from a PCB
	 * @throws Exception
	 */
	static void release() throws Exception{
		// Handle the command			
		String[] params = splitParameters(userCommand);
		if (params.length < 2) throw new Exception("error");
		
		// get the RCB's name
		String name = params[1];
		RCB requestedRCB = RCBMap.get(name);
		if (requestedRCB == null)throw new Exception("error");
		
		// remove RCB from PCB
		freeResource(currentProcess, requestedRCB);
		currentProcess.resource.remove(requestedRCB);
		
		scheduler();
	}
	
	/**
	 * method to handle time out request
	 * @throws Exception
	 */
	static void timeOut() throws Exception{
		// put current Process into last position in ready list
		RL.remove(currentProcess);
		RL.add(currentProcess);
		currentProcess.statusType = 1;
		
		scheduler();
	}
	
	/**
	 * method to printout the current process's name
	 */
	static void printResult(){
		System.out.println(String.format(PROCESS_RUNNING, currentProcess.name));
	}
	
	/**
	 * handle all the commands;
	 */
	static void execute(){
		try{
			String commandTypeString = getFirstWord(userCommand);
			COMMAND_TYPE commandType = getCommandType(commandTypeString);
			switch (commandType){
			case CR:{
				create();
				printResult();
				break;
			}
			case DE:{
				destroy();
				printResult();
				break;
			}
			
			case QUIT: {
				isWorking = false;
				System.out.println(QUIT_STRING);
				break;
			}
			
			case REQ: {
				request();
				printResult();
				break;
			}
			
			case REL: {
				release();
				printResult();
				break;
			}
			
			case INIT: {
				init(); 
				//printResult();
				break;
			}
			
			case INVALID:{
				System.out.println(INVALID_COMMAND_STRING);
				break;
			}
			
			case TO:{
				timeOut();
				printResult();
				break;
			}
			
			case BLANK:{
				System.out.println();	
				break;
			}
			
			case RIO:{
				requestIO();
				printResult();
				break;
			}
			
			case IOC:{
				IOcompletion();
				printResult();
				break;
			}
				
			default:
				break;
			}
		} catch (Exception e){
			System.out.println(e.getMessage());
			printResult();
		}
	}
	
	/**
	 * method the handle ioc command
	 * @throws Exception
	 */
	private static void IOcompletion() throws Exception{
		if (IO.waitList.size() == 0) throw new Exception("error");
		
		//  get the first PCB blocked
		PCB p = IO.waitList.remove(0);
		
		// add it back to ready list
		p.statusType = 1;
		p.statusList = null;
		RL.add(p);
		
		scheduler();
	}

	/**
	 * method to handle rio command
	 */
	private static void requestIO() {
		// block the current process
		currentProcess.statusType = 0;
		currentProcess.statusList = IO;
		RL.remove(currentProcess);
		IO.waitList.add(currentProcess);
		
		scheduler();
	}

	/**
	 * method to get the type of command
	 * @param commandTypeString
	 * @return COMMAND_TYPE
	 */
	static COMMAND_TYPE getCommandType(String commandTypeString){
		if (commandTypeString.equalsIgnoreCase("cr")) {
			return COMMAND_TYPE.CR;
		} else if (commandTypeString.equalsIgnoreCase("de")) {
			return COMMAND_TYPE.DE;
		} else if (commandTypeString.equalsIgnoreCase("quit")) {
			return COMMAND_TYPE.QUIT;
		} else if (commandTypeString.equalsIgnoreCase("to")) {
			return COMMAND_TYPE.TO;
		} else if (commandTypeString.equalsIgnoreCase("req")) {
			return COMMAND_TYPE.REQ;
		} else if (commandTypeString.equalsIgnoreCase("rel")) {
			return COMMAND_TYPE.REL;
		} else if (commandTypeString.equalsIgnoreCase("init")) {
			return COMMAND_TYPE.INIT;
		} else if (commandTypeString.equalsIgnoreCase("")) {
			return COMMAND_TYPE.BLANK;
		} else if (commandTypeString.equalsIgnoreCase("rio")) {
			return COMMAND_TYPE.RIO;
		} else if (commandTypeString.equalsIgnoreCase("ioc")) {
			return COMMAND_TYPE.IOC;
		} else {
			return COMMAND_TYPE.INVALID;
		}
	}
	
	/**
	 * split the string into words without space
	 * @param commandParametersString
	 * @return
	 */
	private static String[] splitParameters(String commandParametersString) {
		String[] parameters = commandParametersString.trim().split("\\s+");
		return parameters;
	}
	
	/**
	 * get the first word in a string
	 * @param userCommand
	 * @return
	 */
	private static String getFirstWord(String userCommand) {
		String[] words = userCommand.trim().split(" ");
		String command = words[0];
		return command;
	}
	
	public static void main(String args[]){
		Scanner sc = new Scanner(System.in);

		init();
		
		while (isWorking){
			userCommand = sc.nextLine();
			execute();
		}
	}
}
