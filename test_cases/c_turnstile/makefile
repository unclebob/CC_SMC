all : clean turnstile_test
	./turnstile_test

clean : 
	rm -f turnstile_test *.o twoCoinTurnstile.h twoCoinTurnstile.c

twoCoinTurnstile.o : twoCoinTurnstile.h twoCoinTurnstile.c turnstileActions.h

turnstile_test.o : turnstile_test.c turnstileActions.h twoCoinTurnstile.h

turnstile_test : turnstile_test.o twoCoinTurnstile.o

twoCoinTurnstile.h twoCoinTurnstile.c : twoCoinTurnstile.sm
	java -jar ../../build/jar/smc.jar -l c twoCoinTurnstile.sm

