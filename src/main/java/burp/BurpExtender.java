package burp;

import java.awt.Component;
import java.io.PrintStream;

import directory.passive.burp.BurpBodyExtractor;
import directory.passive.burp.decoder.PogoDecoder;

public class BurpExtender implements IBurpExtender, IMessageEditorTabFactory {
	private PrintStream out;
	private IBurpExtenderCallbacks callbacks;

	@Override
	public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
		this.callbacks = callbacks;
		callbacks.setExtensionName("Pokemongo decoder");
		callbacks.registerMessageEditorTabFactory(this);
		out = new PrintStream(callbacks.getStderr());
	}

	@Override
	public IMessageEditorTab createNewInstance(IMessageEditorController controller, boolean editable) {
		IExtensionHelpers helpers = callbacks.getHelpers();
		PogoDecoder decoder = new PogoDecoder(new BurpBodyExtractor(helpers));
		TheTab tab = new TheTab(controller, editable, decoder);
		return tab;
	}

	class TheTab implements IMessageEditorTab {
		private final IMessageEditorController controller;
		private final ITextEditor txtInput;
		private PogoDecoder decoder;

		private byte[] currentMessage = "COFFEE".getBytes();

		public TheTab(IMessageEditorController controller, boolean editable, PogoDecoder decoder) {
			if (controller == null || decoder == null) {
				throw new IllegalArgumentException();
			}
			this.controller = controller;
			this.decoder = decoder;
			txtInput = callbacks.createTextEditor();
			txtInput.setEditable(editable);
			txtInput.setText(currentMessage);
		}

		@Override
		public String getTabCaption() {
			return "Pokemon";
		}

		@Override
		public Component getUiComponent() {
			Component c = txtInput.getComponent();
			return c;
		}

		@Override
		public boolean isEnabled(byte[] content, boolean isRequest) {
			if (controller == null) {
				throw new Error();
			}
			try {
				IHttpService service = controller.getHttpService();
				if (service != null) {
					String host = service.getHost();
					if (host != null) {
						return host.contains("pgorelease.nianticlabs.com");
					}
				}
			} catch (NullPointerException e) {
				return true;
			}
			return true;
		}

		@Override
		public void setMessage(byte[] content, boolean isRequest) {
			if (isRequest) {
				decoder.decode(content, null);
				currentMessage = decoder.getRequestDescription().asBytes();
			} else {
				decoder.decode(controller.getRequest(), controller.getResponse());
				currentMessage = decoder.getResponseDescription().asBytes();
			}
			out.println("setting text");
			txtInput.setText(currentMessage);
		}

		@Override
		public byte[] getMessage() {
			return currentMessage;
		}

		@Override
		public boolean isModified() {
			return txtInput.isTextModified();
		}

		@Override
		public byte[] getSelectedData() {
			return txtInput.getSelectedText();
		}
	}
}
