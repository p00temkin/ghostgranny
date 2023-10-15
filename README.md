## GHOSTGRANNY

Magical elder which allows you to crontab your gotchi caretaking.

![alt text](https://github.com/p00temkin/ghostgranny/blob/master/img/ghostgran.png?raw=true)

### How it works

Being part of the Aavegotchi ecosystem typically includes petting your gotchi (ERC721 NFT on the Polygon network) regularly. This can be done every 12 hours and practically means you need to make a call to the interact() function on the AavegotchiFacet of the Diamond contract. This interaction is gas efficient but does require the petting account to have funds to cover gas costs (a few MATIC should be sufficient for a long time). More about the Diamond standard here: <https://docs.aavegotchi.com/overview/diamond-standard>

Typically the gotchi owner makes this Diamond interact() call using the official web UI at <https://app.aavegotchi.com/> with a browser wallet. Although this is a nice daily/evening routine, it can be inconvenient at times which is where ghostgranny comes in. ghostgranny acts as a backup and continously monitors the owners gotchis using the Aavegotchi subgraph, available here: <https://thegraph.com/hosted-service/subgraph/aavegotchi/aavegotchi-core-matic>

ghostgranny is a bundled java application which interacts with Polygon using the web3j wrapper library 'forestfish'

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
   mv target/ghostgranny-0.0.1-SNAPSHOT-jar-with-dependencies.jar ghostgranny.jar
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

The above commands will store the private key in a local wallet file named 'grannywallet' and print out the public address of the ghostgranny account, so no need to use these parameters moving forward. Next send a few MATIC to this ghostgranny account.

Second step is to allow this new ghostgranny account the ability to pet the gotchis, which is something only the gotchi owner can do:

- Access <https://louper.dev/diamond/0x86935F11C86623deC8a25696E1C19a8659CbF95d?network=polygon>
- Scroll down until you see 'AavegotchiFacet'
- Select the write option and connect with the gotchi owner account (using browser wallet)
- Select the setPetOperatorForAll method
- Enter the ghostgranny public account address as _operator
- Check the _bool approved value

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
 2022-08-05 18:04:18,042 component_class=EVMBlockChainConnector thread=main log_level="INFO" node URL https://matic-mainnet.chainstacklabs.com looks fine for POLYGON, will use it
 2022-08-05 18:04:18,042 component_class=EVMBlockChainConnector thread=main log_level="INFO" node version='bor/v0.2.16-stable/linux-amd64/go1.18.1', response_time=412 ms
 2022-08-05 18:04:18,095 component_class=Start thread=main log_level="INFO" Ready to move with granny wallet 0xFFFF
 2022-08-05 18:04:18,096 component_class=GraphQLUtils thread=main log_level="INFO" Making GraphQL query towards https://api.thegraph.com/subgraphs/name/aavegotchi/aavegotchi-core-matic, theGraphErrorCount=0, retryLimit=180
 2022-08-05 18:04:18,750 component_class=Start thread=main log_level="INFO" gotchi forestfish001 has kinship=715, and needs love in 17100 seconds
 2022-08-05 18:04:18,750 component_class=Start thread=main log_level="INFO" gotchi forestfish002 has kinship=715, and needs love in 17100 seconds
 2022-08-05 18:04:18,750 component_class=Start thread=main log_level="INFO" gotchi forestfish003 has kinship=637, and needs love in 17100 seconds
 2022-08-05 18:04:18,750 component_class=Start thread=main log_level="INFO" .... sleeping 60 seconds
   ```

### Advanced options

By default a random Polygon RPC Provider URL will be used by ghostgranny. If you want to setup multiple grannys you can specify the RPC Provider URL using the '-p' option and have two ghostgrannys running in parallel with different RPC nodes. You can also enable the high availablily setting '-x' and configure one granny with an extra sleep delay '-e'.

### Note on Safety

Recommended setup is to keep the ghostgranny account separate from the gotchi owner account. This way you can secure your gotchi account with a Trezor/Ledger and only risk the few MATIC in your grannys hot wallet.

### Additional useful options/resources

- <https://programmablewealth.com/how-to-automate-aavegotchi-petting-with-pet-operator-and-gelato/>
- <https://www.gotchivault.com/>
- <https://gotchicare.com/>

### Support/Donate

To support this project directly:

   ```
   Ethereum/EVM: forestfish.x / 0x207d907768Df538F32f0F642a281416657692743
   Algorand: forestfish.x / 3LW6KZ5WZ22KAK4KV2G73H4HL2XBD3PD3Z5ZOSKFWGRWZDB5DTDCXE6NYU
   ```

Or please consider donating to EFF:
[Electronic Frontier Foundation](https://supporters.eff.org/donate)

