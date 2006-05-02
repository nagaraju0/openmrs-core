package org.openmrs.arden;

import antlr.ANTLRHashString;
import antlr.InputBuffer;
import antlr.LexerSharedInputState;
import antlr.Token;
import org.openmrs.arden.ArdenBaseLexer;
import org.openmrs.arden.parser.ArdenToken;


import java.io.InputStream;
import java.io.Reader;

/**
 * Custom lexer for the Arden grammar.  Extends the base lexer generated by ANTLR
 * in order to keep the grammar source file clean.
 */
class ArdenLexer extends ArdenBaseLexer {
	private boolean possibleID = false;

	public ArdenLexer(InputStream in) {
		super( in );
	}

	public ArdenLexer(Reader in) {
		super( in );
	}

	public ArdenLexer(InputBuffer ib) {
		super( ib );
	}

	public ArdenLexer(LexerSharedInputState state) {
		super( state );
	}

	public void setTokenObjectClass(String cl) {
		// Ignore the token class name parameter, and use a specific token class.
		super.setTokenObjectClass(ArdenToken.class.getName() );
	}

	protected void setPossibleID(boolean possibleID) {
		this.possibleID = possibleID;
	}

	protected Token makeToken(int i) {
		ArdenToken token = ( ArdenToken ) super.makeToken( i );
		token.setPossibleID( possibleID );
		possibleID = false;
		return token;
	}

	/**
	 * Test the text passed in against the literals table
	 * Override this method to perform a different literals test
	 * This is used primarily when you want to test a portion of
	 * a token.
	 */
	public int testLiteralsTable(String text, int ttype) {
		ANTLRHashString s = new ANTLRHashString( text, this );
		Integer literalsIndex = ( Integer ) literals.get( s );
		if ( literalsIndex != null ) {
			ttype = literalsIndex.intValue();
		}
		return ttype;
	}
}
