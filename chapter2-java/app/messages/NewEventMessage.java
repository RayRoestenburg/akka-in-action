package messages;

public class NewEventMessage {

	private String name;
	private int numberOfSeats;
	
	public NewEventMessage(String name, int numberOfSeats) {
		super();
		this.name = name;
		this.numberOfSeats = numberOfSeats;
	}
	
	public String getName() {
		return name;
	}
	
	public int getNumberOfSeats() {
		return numberOfSeats;
	}
	
	
	
}
