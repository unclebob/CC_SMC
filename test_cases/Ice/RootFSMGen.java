public abstract class RootFSMGen {
	public abstract void unhandledTransition(String state, String event);

	private enum State {autoBatch, batchSplashAuto, batchSplashManual, determiningUserMode, end, gettingAutoBatch, gettingManualBatch, init, login, manualBatch, pageAutoBatch, pageAutoBatchStopped, pageManualBatch, processingAutoBatch, processingAutoBatchStopped, processingManualBatch}

	private enum Event {cancel, init, auto, nextBatchFound, select, refresh, login, manual, requeue, openPage, exit, goBack, stop, reject, batchesFound, itemChanged, redisplay, noBatchFound, ok, complete, setZone, assign}

	private State state = State.init;

	private void setState(State s) {
		state = s;
	}

	public void cancel() {
		handleEvent(Event.cancel);
	}

	public void init() {
		handleEvent(Event.init);
	}

	public void auto() {
		handleEvent(Event.auto);
	}

	public void nextBatchFound() {
		handleEvent(Event.nextBatchFound);
	}

	public void select() {
		handleEvent(Event.select);
	}

	public void refresh() {
		handleEvent(Event.refresh);
	}

	public void login() {
		handleEvent(Event.login);
	}

	public void manual() {
		handleEvent(Event.manual);
	}

	public void requeue() {
		handleEvent(Event.requeue);
	}

	public void openPage() {
		handleEvent(Event.openPage);
	}

	public void exit() {
		handleEvent(Event.exit);
	}

	public void goBack() {
		handleEvent(Event.goBack);
	}

	public void stop() {
		handleEvent(Event.stop);
	}

	public void reject() {
		handleEvent(Event.reject);
	}

	public void batchesFound() {
		handleEvent(Event.batchesFound);
	}

	public void itemChanged() {
		handleEvent(Event.itemChanged);
	}

	public void redisplay() {
		handleEvent(Event.redisplay);
	}

	public void noBatchFound() {
		handleEvent(Event.noBatchFound);
	}

	public void ok() {
		handleEvent(Event.ok);
	}

	public void complete() {
		handleEvent(Event.complete);
	}

	public void setZone() {
		handleEvent(Event.setZone);
	}

	public void assign() {
		handleEvent(Event.assign);
	}

	private void handleEvent(Event event) {
		switch (state) {
			case autoBatch:
				switch (event) {
					case manual:
						setState(State.gettingManualBatch);
						isBatchAvailable();
						createSelector();
						break;
					case select:
						setState(State.gettingAutoBatch);
						getNextAutoBatch();
						createSelector();
						break;
					case itemChanged:
						setState(State.autoBatch);
						setUserAuto();
						displayThumbnailAuto();
						workTypeItemChanged();
						break;
					case redisplay:
						setState(State.autoBatch);
						setUserAuto();
						displayThumbnailAuto();
						displayThumbnailAuto();
						break;
					case exit:
						setState(State.end);
						exitProgram();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case batchSplashAuto:
				switch (event) {
					case ok:
						setState(State.processingAutoBatch);
						hideBatchSplashScreen();
						allMode();
						initBatch();
						displayAutoThumbnailProcessing();
						break;
					case complete:
						setState(State.gettingAutoBatch);
						hideBatchSplashScreen();
						getNextAutoBatch();
						completeBatch();
						hideThumbnailScreen();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case batchSplashManual:
				switch (event) {
					case ok:
						setState(State.processingManualBatch);
						hideBatchSplashScreen();
						allMode();
						initBatch();
						displayManualThumbnailProcessing();
						break;
					case complete:
						setState(State.determiningUserMode);
						hideBatchSplashScreen();
						cleanupThumbnails();
						checkUserState();
						completeBatch();
						hideThumbnailScreen();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case determiningUserMode:
				switch (event) {
					case auto:
						setState(State.autoBatch);
						setUserAuto();
						displayThumbnailAuto();
						break;
					case manual:
						setState(State.gettingManualBatch);
						isBatchAvailable();
						createSelector();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case end:
				switch (event) {
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case gettingAutoBatch:
				switch (event) {
					case nextBatchFound:
						setState(State.batchSplashAuto);
						displayBatchSplashScreen();
						break;
					case noBatchFound:
						setState(State.determiningUserMode);
						cleanupThumbnails();
						checkUserState();
						noBatchDialog();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case gettingManualBatch:
				switch (event) {
					case batchesFound:
						setState(State.manualBatch);
						setUserManual();
						displayThumbnailManual();
						break;
					case noBatchFound:
						setState(State.autoBatch);
						setUserAuto();
						displayThumbnailAuto();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case init:
				switch (event) {
					case init:
						setState(State.login);
						displayLoginScreen();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case login:
				switch (event) {
					case login:
						setState(State.determiningUserMode);
						hideLoginScreen();
						cleanupThumbnails();
						checkUserState();
						break;
					case cancel:
						setState(State.end);
						hideLoginScreen();
						exitProgram();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case manualBatch:
				switch (event) {
					case auto:
						setState(State.autoBatch);
						setUserAuto();
						displayThumbnailAuto();
						break;
					case refresh:
						setState(State.gettingManualBatch);
						isBatchAvailable();
						break;
					case select:
						setState(State.batchSplashManual);
						displayBatchSplashScreen();
						selectManualBatch();
						break;
					case redisplay:
						setState(State.manualBatch);
						setUserManual();
						displayThumbnailManual();
						displayThumbnailManual();
						break;
					case exit:
						setState(State.end);
						exitProgram();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case pageAutoBatch:
				switch (event) {
					case goBack:
						setState(State.processingAutoBatch);
						hidePageScreen();
						displayAutoThumbnailProcessing();
						break;
					case assign:
						setState(State.page);
						hidePageScreen();
						displayPageScreen();
						assignPage();
						redisplayPageScreen();
						break;
					case setZone:
						setState(State.page);
						hidePageScreen();
						displayPageScreen();
						assignZone();
						redisplayPageScreen();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case pageAutoBatchStopped:
				switch (event) {
					case goBack:
						setState(State.processingAutoBatchStopped);
						hidePageScreen();
						displayAutoThumbnailProcessing();
						break;
					case assign:
						setState(State.page);
						hidePageScreen();
						displayPageScreen();
						assignPage();
						redisplayPageScreen();
						break;
					case setZone:
						setState(State.page);
						hidePageScreen();
						displayPageScreen();
						assignZone();
						redisplayPageScreen();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case pageManualBatch:
				switch (event) {
					case goBack:
						setState(State.processingManualBatch);
						hidePageScreen();
						displayManualThumbnailProcessing();
						break;
					case assign:
						setState(State.page);
						hidePageScreen();
						displayPageScreen();
						assignPage();
						redisplayPageScreen();
						break;
					case setZone:
						setState(State.page);
						hidePageScreen();
						displayPageScreen();
						assignZone();
						redisplayPageScreen();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case processingAutoBatch:
				switch (event) {
					case stop:
						setState(State.processingAutoBatchStopped);
						hideThumbnailScreen();
						break;
					case complete:
						setState(State.gettingAutoBatch);
						hideThumbnailScreen();
						getNextAutoBatch();
						completeBatch();
						cleanupBatch();
						break;
					case reject:
						setState(State.gettingAutoBatch);
						hideThumbnailScreen();
						getNextAutoBatch();
						rejectBatch();
						cleanupBatch();
						break;
					case openPage:
						setState(State.pageAutoBatch);
						hideThumbnailScreen();
						displayPageScreen();
						break;
					case redisplay:
						setState(State.processingAutoBatch);
						hideThumbnailScreen();
						displayAutoThumbnailProcessing();
						break;
					case ok:
						setState(State.processingBatch);
						hideThumbnailScreen();
						break;
					case cancel:
						setState(State.processingBatch);
						hideThumbnailScreen();
						break;
					case requeue:
						setState(State.determiningUserMode);
						hideThumbnailScreen();
						cleanupThumbnails();
						checkUserState();
						requeueBatch();
						cleanupBatch();
						break;
					case assign:
						setState(State.processingBatch);
						hideThumbnailScreen();
						assignPage();
						break;
					case exit:
						setState(State.end);
						hideThumbnailScreen();
						exitProgram();
						requeueBatch();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case processingAutoBatchStopped:
				switch (event) {
					case complete:
						setState(State.determiningUserMode);
						hideThumbnailScreen();
						cleanupThumbnails();
						checkUserState();
						completeBatch();
						cleanupBatch();
						break;
					case reject:
						setState(State.determiningUserMode);
						hideThumbnailScreen();
						cleanupThumbnails();
						checkUserState();
						rejectBatch();
						cleanupBatch();
						break;
					case openPage:
						setState(State.pageAutoBatchStopped);
						hideThumbnailScreen();
						displayPageScreen();
						break;
					case stop:
						setState(State.processingAutoBatch);
						hideThumbnailScreen();
						break;
					case redisplay:
						setState(State.processingAutoBatchStopped);
						hideThumbnailScreen();
						displayAutoThumbnailProcessing();
						break;
					case ok:
						setState(State.processingBatch);
						hideThumbnailScreen();
						break;
					case cancel:
						setState(State.processingBatch);
						hideThumbnailScreen();
						break;
					case requeue:
						setState(State.determiningUserMode);
						hideThumbnailScreen();
						cleanupThumbnails();
						checkUserState();
						requeueBatch();
						cleanupBatch();
						break;
					case assign:
						setState(State.processingBatch);
						hideThumbnailScreen();
						assignPage();
						break;
					case exit:
						setState(State.end);
						hideThumbnailScreen();
						exitProgram();
						requeueBatch();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
			case processingManualBatch:
				switch (event) {
					case openPage:
						setState(State.pageManualBatch);
						hideThumbnailScreen();
						displayPageScreen();
						break;
					case redisplay:
						setState(State.processingManualBatch);
						hideThumbnailScreen();
						displayManualThumbnailProcessing();
						break;
					case ok:
						setState(State.processingBatch);
						hideThumbnailScreen();
						break;
					case cancel:
						setState(State.processingBatch);
						hideThumbnailScreen();
						break;
					case complete:
						setState(State.determiningUserMode);
						hideThumbnailScreen();
						cleanupThumbnails();
						checkUserState();
						completeBatch();
						cleanupBatch();
						break;
					case requeue:
						setState(State.determiningUserMode);
						hideThumbnailScreen();
						cleanupThumbnails();
						checkUserState();
						requeueBatch();
						cleanupBatch();
						break;
					case reject:
						setState(State.determiningUserMode);
						hideThumbnailScreen();
						cleanupThumbnails();
						checkUserState();
						rejectBatch();
						cleanupBatch();
						break;
					case assign:
						setState(State.processingBatch);
						hideThumbnailScreen();
						assignPage();
						break;
					case exit:
						setState(State.end);
						hideThumbnailScreen();
						exitProgram();
						requeueBatch();
						break;
					default:
						unhandledTransition(state.name(), event.name());
						break;
				}
				break;
		}
	}

	protected abstract void assignPage();

	protected abstract void isBatchAvailable();

	protected abstract void cleanupBatch();

	protected abstract void initBatch();

	protected abstract void hideBatchSplashScreen();

	protected abstract void displayThumbnailAuto();

	protected abstract void exitProgram();

	protected abstract void rejectBatch();

	protected abstract void redisplayPageScreen();

	protected abstract void displayThumbnailManual();

	protected abstract void assignZone();

	protected abstract void workTypeItemChanged();

	protected abstract void noBatchDialog();

	protected abstract void requeueBatch();

	protected abstract void checkUserState();

	protected abstract void displayAutoThumbnailProcessing();

	protected abstract void completeBatch();

	protected abstract void cleanupThumbnails();

	protected abstract void selectManualBatch();

	protected abstract void displayLoginScreen();

	protected abstract void displayBatchSplashScreen();

	protected abstract void hideLoginScreen();

	protected abstract void displayPageScreen();

	protected abstract void hidePageScreen();

	protected abstract void setUserManual();

	protected abstract void hideThumbnailScreen();

	protected abstract void getNextAutoBatch();

	protected abstract void createSelector();

	protected abstract void displayManualThumbnailProcessing();

	protected abstract void allMode();

	protected abstract void setUserAuto();
}
