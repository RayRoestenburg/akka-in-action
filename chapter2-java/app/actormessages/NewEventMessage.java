package actormessages;

public class NewEventMessage {

	private final String name;
	private final int numberOfSeats;
	
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
