all : clean two_coin_turnstile.go turnstile_test.go
	go vet .
	go test .

clean :
	rm -f two_coin_turnstile.go

two_coin_turnstile.go : two_coin_turnstile.sm
	java -jar ../../build/jar/smc.jar -l Go $?
	go fmt $@


