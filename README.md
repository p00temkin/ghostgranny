## GhostGranny 

Magical elder which allows you to crontab your gotchi caretaking.

![alt text](https://github.com/p00temkin/ghostgranny/blob/master/img/ghostgranny.png?raw=true)

### Prerequisites

   ```
git clone https://github.com/p00temkin/forestfish
mvn clean package install
   ```

### Building the application

   ```
   mvn clean package
   mv target/ghostgranny-0.0.1-SNAPSHOT-jar-with-dependencies.jar ghostgranny.jar
   ```
   
### Usage

With a wallet address 0xABC, a MATIC RPC Provider URL (from Infura, Pokt, QuickNode, ..), start an infinate petting loop by running:

   ```
   java -jar ./ghostgranny.jar -w 0xABC -p "https://polygon-mainnet.infura.io/v3/abc"
   ```
   
The tool expects an existing JSON private key in a local folder named maticwallet. If you want the tool to create the wallet for you, use the --walletmnemonic (-m) or --walletprivkey (-k) option. The tool makes use of api.thegraph.com for checking state of the gotchis. 

Options:
   ```
 -p,--providerurl <arg>      MATIC Provider URL (infura etc)
 -t,--tokenids <arg>         csv decimal list of gotchi token ids (used unless wallet specified)
 -m,--walletmnemonic <arg>   Wallet mnemonic (ghostgranny wallet)
 -k,--walletprivkey <arg>    Wallet private key (ghostgranny wallet)
 -w,--wallet <arg>           Wallet address (public address of wallet which owns the gotchis)
 -f,--petmethodid <arg>      Aavegotchi pet method id/function address (default 0x22c67519)
 -l,--gaslimit <arg>         Gas limit for the MATIC network
 -x,--hamode                 High Availability mode (removes warning messages caused by running multiple grannies)
 -o,--forcepet               Override timeout check and pet all owned gotchis instantly
 -e,--extradelay             Extra sleep delay to be used in hamode to avoid granny clash
   ```

### Note on Safety
Recommended setup is to run ghostgranny with a wallet which does not own any gotchis. Instead, fund a separate wallet with a small amount of MATIC to cover gas fees, and enable to "Pet Operator" for your ghostgranny wallet address as described in https://programmablewealth.com/how-to-automate-aavegotchi-petting-with-pet-operator-and-gelato/ under the section "Enable Pet Operator permission with louper.dev".  
   
### Support/Donate

forestfish.x / 0x207d907768Df538F32f0F642a281416657692743
