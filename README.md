## GHOSTGRANNY

Magical elder which allows you to crontab your gotchi caretaking.

![alt text](https://github.com/p00temkin/ghostgranny/blob/master/img/ghostgran.png?raw=true)

### How it works

Being part of the Aavegotchi ecosystem typically includes petting your gotchi (ERC721 NFT on the Polygon network) regularly. This can be done every 12 hours and means you need to make a call to the interact() function on the AavegotchiFacet of the Diamond contract. This interaction is gas efficient but does require the petting account to have funds to cover gas costs. More about the Diamond standard here: <https://docs.aavegotchi.com/overview/diamond-standard>

Typically the gotchi owner makes this Diamond interact() call using the official web UI at <https://app.aavegotchi.com/> with a browser wallet. Although this is a nice daily/evening routine, it can be inconvenient (travel/sickness etc) which is where ghostgranny comes in. ghostgranny acts as a backup and continously monitors the owners gotchis using the Aavegotchi subgraph. Note that Aavegotchi and contracts have migrated from Polygon to Base as of July 25, 2025.

ghostgranny is a bundled java application which interacts with the Diamond contract using the web3j wrapper library 'forestfish'

### Prerequisites

[Java 17+, Maven 3.x]

   ```
 java -version # jvm 1.8+ required
 mvn -version # maven 3.x required
 git clone https://github.com/p00temkin/forestfish
 mvn clean package install
   ```

### Building the application

   ```
   mvn clean package
   mv target/ghostgranny-<version>-jar-with-dependencies.jar ghostgranny.jar
   ```

### Setup

First step is to import/create and fund a ghostgranny account. You can create a new account by running the following:

   ```
   java -jar ./ghostgranny.jar -m "12/24 word seed phrase"
   ```

or using a private key:

   ```
   java -jar ./ghostgranny.jar -k "64 hex char private key"
   ```

The above commands will store the private key in a local wallet file named 'grannywallet' and print out the public address of the ghostgranny account, so no need to use these parameters moving forward. Next send a minimal amount of funds to this ghostgranny account (to cover the contract calls).

Second step is to allow this new ghostgranny account the ability to pet the gotchis, which is something only the gotchi owner can do:

- Access <https://louper.dev/diamond/0x86935F11C86623deC8a25696E1C19a8659CbF95d?network=base>
- Scroll down until you see 'AavegotchiFacet'
- Select the write option and connect with the gotchi owner account (using browser wallet)
- Select the setPetOperatorForAll method
- Enter the ghostgranny public account address as _operator
- Check the _bool approved value

Note that at the time of writing this is not yet supported on Base, so instead head over to
 - https://basescan.org/address/0xa99c4b08201f2913db8d28e71d020c4298f29dbf#multipleProxyContract .. Write Contract
 - Select AavegotchiFacet on the left hand side
 - setPetOperatorForAll (0xcd675d57) to your ghostgranny wallet address
 - _approved to 'true'
 - Write
 - Verify by calling isPetOperatorForAll using the owner address (_owner) and your ghostgranny wallet address (_operator)

### Usage

For gotchi owner account 0xABC, start an infinite petting loop by running:

   ```
   java -jar ./ghostgranny.jar -g 0xABC
   ```

Options:

   ```
-e,--extradelay <arg>				Extra delay in seconds
-g,--gotchiowner <arg>				Gotchi owner account address
-i,--petmethodid <arg>				Aavegotchi interact() method id (default: 0x22c67519)
-k,--ggprivkey <arg>				Create ghostgranny account using private key
-m,--ggmnemonic <arg>				Create ghostgranny account using mnemonic
-p,--providerurl <arg>				MATIC/Polygon Provider URL (infura etc)
-c,--gotchicatchupthreshold <arg>	Delay pet action to allow for gotchis soon in need to catch up (default 3600 seconds)
-x,--hamode              			High Availability mode (removes warning messages caused by running multiple grannies)
   ```

ghostgranny will continously output progress and will switch RPC node if needed:

   ```
2025-08-04 10:45:36,521 component_class=EVMBlockChainConnector thread=main log_level="INFO" node URL https://1rpc.io/base looks fine for BASE, will use it
2025-08-04 10:45:36,524 component_class=EVMBlockChainConnector thread=main log_level="INFO" latestblock='33753903', response_time=470 ms
2025-08-04 10:45:36,759 component_class=EVMUtils thread=main log_level="INFO" wallet 0x... has sufficient funds: 0.0118....
2025-08-04 10:45:36,759 component_class=Start thread=main log_level="INFO" Ready to move with granny wallet 0x...
2025-08-04 10:45:36,760 component_class=GrannyUtils thread=main log_level="INFO" firstAttempt: true
2025-08-04 10:45:36,761 component_class=GraphQLUtils thread=main log_level="INFO" Making GraphQL query towards https://subgraph.satsuma-prod.com/tWYl5n5y04oz/aavegotchi/aavegotchi-core-base/api
2025-08-04 10:45:37,780 component_class=GrannyUtils thread=main log_level="INFO" Turns out we have 10 gotchis to take care of ..
2025-08-04 10:45:37,780 component_class=GrannyUtils thread=main log_level="INFO" ghostgranny notes:
2025-08-04 10:45:37,781 component_class=GrannyUtils thread=main log_level="INFO" ----------------------------
2025-08-04 10:45:37,782 component_class=GrannyUtils thread=main log_level="INFO" Gotchis in need of hugs            : ...
   ```

### Advanced options

By default a random RPC Provider URL will be used by ghostgranny. If you want to setup multiple grannys you can specify the RPC Provider URL using the '-p' option and have two ghostgrannys running in parallel with different RPC nodes. You can also enable the high availablily setting '-x' and configure one granny with an extra sleep delay '-e'.

### Note on Safety

Recommended setup is to keep the ghostgranny account separate from the gotchi owner account. This way you can secure your gotchi account with a Trezor/Ledger and only risk the mninimal funds in your grannys hot wallet.

### Additional useful options/resources

- <https://programmablewealth.com/how-to-automate-aavegotchi-petting-with-pet-operator-and-gelato/>
- <https://www.gotchivault.com/>

### Support/Donate

To support this project directly:

   ```
   Ethereum/EVM: forestfish.x / 0x207d907768Df538F32f0F642a281416657692743
   Algorand: forestfish.x / 3LW6KZ5WZ22KAK4KV2G73H4HL2XBD3PD3Z5ZOSKFWGRWZDB5DTDCXE6NYU
   ```

Or please consider donating to EFF:
[Electronic Frontier Foundation](https://supporters.eff.org/donate)

