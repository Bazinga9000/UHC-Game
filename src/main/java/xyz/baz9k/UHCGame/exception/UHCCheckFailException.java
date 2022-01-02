package xyz.baz9k.UHCGame.exception;

import xyz.baz9k.UHCGame.util.ComponentUtils.Key;

public class UHCCheckFailException extends UHCException {

    public UHCCheckFailException(Key key) {
        super(key);
    }
    
}
