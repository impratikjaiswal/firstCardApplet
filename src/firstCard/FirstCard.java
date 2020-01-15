package firstCard;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;

public class FirstCard extends Applet {

	// code of CLA byte in the command APDU header
	final static byte Wallet_CLA = (byte) 0xB0;

	// codes of INS byte in the command APDU header
	final static byte VERIFY = (byte) 0x20;
	final static byte CREDIT = (byte) 0x30;
	final static byte DEBIT = (byte) 0x40;
	final static byte GET_BALANCE = (byte) 0x50;

	// maximum balance
	final static short MAX_BALANCE = 0x7FFF;
	// maximum transaction amount
	final static byte MAX_TRANSACTION_AMOUNT = 127;

	// maximum number of incorrect tries before the
	// PIN is blocked
	final static byte PIN_TRY_LIMIT = (byte) 0x03;
	// maximum size PIN
	final static byte MAX_PIN_SIZE = (byte) 0x08;

	// signal that the PIN verification failed
	final static short SW_VERIFICATION_FAILED = 0x6300;
	// signal the the PIN validation is required
	// for a credit or a debit transaction
	final static short SW_PIN_VERIFICATION_REQUIRED = 0x6301;
	// signal invalid transaction amount
	// amount > MAX_TRANSACTION_AMOUNT or amount < 0
	final static short sw_invalid_transaction_amount = 0x6a83;

	// signal that the balance exceed the maximum
	final static short sw_exceed_maximum_balance = 0x6a84;
	// signal the the balance becomes negative
	final static short sw_negative_balance = 0x6a85;

	/* instance variables declaration */
	OwnerPIN pin;
	short balance;

	private FirstCard(byte[] bArray, short bOffset, byte bLength) {

		// It is good programming practice to allocate
		// all the memory that an applet needs during
		// its lifetime inside the constructor
		pin = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_SIZE);

		// The installation parameters contain the PIN
		// initialization value
		pin.update(bArray, bOffset, bLength);
		register();

	} // end of the constructor

	public static void install(byte bArray[], short bOffset, byte bLength) {
		// create a Wallet applet instance
		new FirstCard(bArray, bOffset, bLength);
	}

	public boolean select() {

		// The applet declines to be selected
		// if the pin is blocked.
		if (pin.getTriesRemaining() == 0)
			return false;

		return true;

	}// end of select method

	public void deselect() {

		// reset the pin value
		pin.reset();

	}

	public void process(APDU apdu) {

		byte buffer[] = apdu.getBuffer();

		// check SELECT APDU command
		if ((buffer[ISO7816.OFFSET_CLA] == 0)
				&& (buffer[ISO7816.OFFSET_INS] == (byte) (0xA4)))
			return;

		// verify the reset of commands have the
		// correct CLA byte, which specifies the
		// command structure
		if (buffer[ISO7816.OFFSET_CLA] != Wallet_CLA)
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);

		switch (buffer[ISO7816.OFFSET_INS]) {
		case GET_BALANCE:
			// getBalance(apdu);
			return;
		case DEBIT:
			// debit(apdu);
			return;
		case CREDIT:
			credit(apdu);
			return;
		case VERIFY:
			// verify(apdu);
			return;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}

	} // end of process method

	private void credit(APDU apdu) {

		// access authentication
		if (!pin.isValidated())
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);

		byte buffer[] = apdu.getBuffer();

		// Lc byte denotes the number of bytes in the
		// data field of the command APDU
		byte numBytes = buffer[ISO7816.OFFSET_LC];

		// indicate that this APDU has incoming data
		// and receive data starting from the offset
		// ISO7816.OFFSET_CDATA following the 5 header
		// bytes.

		byte byteRead = (byte) (apdu.setIncomingAndReceive());

		// it is an error if the number of data bytes
		// read does not match the number in Lc byte
		if ((numBytes != 1) || (byteRead != 1))
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

		// get the credit amount
		byte creditAmount = buffer[ISO7816.OFFSET_CDATA];

		// check the credit amount
		if ((creditAmount > MAX_TRANSACTION_AMOUNT) || (creditAmount < 0))
			ISOException.throwIt(sw_invalid_transaction_amount);

		// check the new balance
		if ((short) (balance + creditAmount) > MAX_BALANCE)
			ISOException.throwIt(sw_exceed_maximum_balance);

		// credit the amount
		balance = (short) (balance + creditAmount);

	} // end of deposit method
}
