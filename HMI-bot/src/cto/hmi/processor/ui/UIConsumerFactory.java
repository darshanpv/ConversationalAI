package cto.hmi.processor.ui;

import cto.hmi.processor.dialogmodel.Dialog;
import cto.hmi.processor.exceptions.RuntimeError;


public abstract class UIConsumerFactory {

	public abstract UIConsumer create() throws RuntimeError;
	public abstract UIConsumer create(Dialog d) throws RuntimeError;
}
