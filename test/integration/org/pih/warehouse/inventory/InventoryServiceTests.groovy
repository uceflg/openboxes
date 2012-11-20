/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 * */
package org.pih.warehouse.inventory


import org.pih.warehouse.product.Product
import org.pih.warehouse.core.Constants
import org.pih.warehouse.core.Location
import org.pih.warehouse.core.LocationType
import org.pih.warehouse.core.Tag;
import org.pih.warehouse.core.User;

import testutils.DbHelper



class InventoryServiceTests extends GroovyTestCase {
    protected def transactionType_consumptionDebit
    protected def  transactionType_inventory
    protected def  transactionType_productInventory
    protected def  transactionType_transferIn
    protected def  transactionType_transferOut
    protected def  bostonLocation
    protected def  haitiLocation
    protected def  warehouseLocationType
    protected def  supplierLocationType
    protected def  acmeLocation
    protected def  bostonInventory
    protected def  haitiInventory
    protected def  aspirinProduct
    protected def  tylenolProduct
    protected def  aspirinItem1
    protected def  aspirinItem2
    protected def tylenolItem
    def transaction1
    def transaction2
    def transaction3
    def transaction4
    def transaction5

    private void basicTestFixture(){
        warehouseLocationType = LocationType.get(Constants.WAREHOUSE_LOCATION_TYPE_ID)
        supplierLocationType = LocationType.get(Constants.SUPPLIER_LOCATION_TYPE_ID)

        // get or create a default location
        acmeLocation = DbHelper.creatLocationIfNotExist("Acme Supply Company", supplierLocationType)

        // create some default warehouses and inventories
        bostonLocation = DbHelper.creatLocationIfNotExist("Boston Location", warehouseLocationType)
        haitiLocation = DbHelper.creatLocationIfNotExist("Haiti Location", warehouseLocationType)

        bostonInventory = DbHelper.createInventory(bostonLocation)
        haitiInventory = DbHelper.createInventory(haitiLocation)

        // create some default transaction types
        transactionType_consumptionDebit = TransactionType.get(Constants.CONSUMPTION_TRANSACTION_TYPE_ID) //id:2
        transactionType_inventory = TransactionType.get(Constants.INVENTORY_TRANSACTION_TYPE_ID) //id:7
        transactionType_productInventory = TransactionType.get(Constants.PRODUCT_INVENTORY_TRANSACTION_TYPE_ID)  //id:11
        transactionType_transferIn =  TransactionType.get(Constants.TRANSFER_IN_TRANSACTION_TYPE_ID) //id:8
        transactionType_transferOut =  TransactionType.get(Constants.TRANSFER_OUT_TRANSACTION_TYPE_ID) //id:9

        // create some products
        aspirinProduct = DbHelper.creatProductIfNotExist("Aspirin" + UUID.randomUUID().toString()[0..5])
        tylenolProduct = DbHelper.creatProductIfNotExist("Tylenol" + UUID.randomUUID().toString()[0..5])

        // create some inventory items
        aspirinItem1 = DbHelper.createInventoryItem(aspirinProduct, "1")
        aspirinItem2 = DbHelper.createInventoryItem(aspirinProduct, "2")
        tylenolItem = DbHelper.createInventoryItem(tylenolProduct, "1")
    }

    private void transactionEntryTestFixture() {

        basicTestFixture()
        // create some transactions
        transaction1 = new Transaction(transactionType: transactionType_inventory,
                transactionDate: new Date() - 5, inventory: bostonInventory)
        transaction2 = new Transaction(transactionType: transactionType_consumptionDebit,
                transactionDate: new Date() - 4, inventory: bostonInventory)
        transaction3 = new Transaction(transactionType: transactionType_productInventory,
                transactionDate: new Date() - 3, inventory: bostonInventory)
        transaction4 = new Transaction(transactionType: transactionType_transferIn,
                transactionDate: new Date() - 2, inventory: bostonInventory, source: haitiLocation)
        transaction5 = new Transaction(transactionType: transactionType_consumptionDebit,
                transactionDate: new Date() - 1, inventory: bostonInventory, destination: haitiLocation)

        // define some aspirin lot 1 transaction entries
        transaction1.addToTransactionEntries(new TransactionEntry(quantity: 10, inventoryItem: aspirinItem1))
        transaction2.addToTransactionEntries(new TransactionEntry(quantity: 2, inventoryItem: aspirinItem1))
        transaction3.addToTransactionEntries(new TransactionEntry(quantity: 100, inventoryItem: aspirinItem1))
        transaction4.addToTransactionEntries(new TransactionEntry(quantity: 24, inventoryItem: aspirinItem1))
        transaction5.addToTransactionEntries(new TransactionEntry(quantity: 30, inventoryItem: aspirinItem1))

        // define some aspirin lot 2 transaction entries
        transaction1.addToTransactionEntries(new TransactionEntry(quantity: 25, inventoryItem: aspirinItem2))
        transaction2.addToTransactionEntries(new TransactionEntry(quantity: 2, inventoryItem: aspirinItem2))
        // even though there is no entry for this lot on this transaction, it is  product inventory transaction so should reset the quantity count
        transaction4.addToTransactionEntries(new TransactionEntry(quantity: 16, inventoryItem: aspirinItem2))
        transaction5.addToTransactionEntries(new TransactionEntry(quantity: 13, inventoryItem: aspirinItem2))

        // define some tylenol lot 1 transaction entries
        transaction1.addToTransactionEntries(new TransactionEntry(quantity: 36, inventoryItem: tylenolItem))
        transaction2.addToTransactionEntries(new TransactionEntry(quantity: 21, inventoryItem: tylenolItem))
        transaction4.addToTransactionEntries(new TransactionEntry(quantity: 33, inventoryItem: tylenolItem))
        transaction5.addToTransactionEntries(new TransactionEntry(quantity: 23, inventoryItem: tylenolItem))

        def transactions = [transaction1, transaction2, transaction3, transaction4, transaction5]
        transactions.each {
            if(!it.save(flush: true)){
              it.errors.allErrors.each {
                    println it
                }
            }
        }


        assert transaction1.id != null
        assert transaction2.id != null
        assert transaction3.id != null
        assert transaction4.id != null
        assert transaction5.id != null
    }

    private void localTransferTestFixture() {
        basicTestFixture()

        transaction1 = new Transaction(transactionType: transactionType_inventory,
                transactionDate: new Date(), inventory: bostonInventory)
        transaction2 = new Transaction(transactionType: transactionType_transferIn,
                transactionDate: new Date(), inventory: bostonInventory, source: "sourceString")
        transaction3 = new Transaction(transactionType: transactionType_transferOut,
                transactionDate: new Date(), inventory: bostonInventory, destination: acmeLocation)
        transaction4 = new Transaction(transactionType: transactionType_transferIn,
                transactionDate: new Date(), inventory: bostonInventory, source: haitiLocation)
        transaction5 = new Transaction(transactionType: transactionType_transferOut,
                transactionDate: new Date(), inventory: bostonInventory, destination: haitiLocation)


    }
	
	
	private void productTagTestFixture() {		
		basicTestFixture()
		User user = User.get(1)
		assertNotNull user 
		println user
		aspirinProduct.addToTags(new Tag(tag: "thistag"))
		aspirinProduct.save(flush:true, failOnError:true)
		tylenolProduct.addToTags(new Tag(tag: "thattag"))
		tylenolProduct.save(flush:true, failOnError:true)
		assertEquals 1, aspirinProduct.tags.size()
		assertEquals 1, tylenolProduct.tags.size()
		assertEquals 2, Tag.list().size()
		
	}

    void test_getQuantityByProductMap() {

        transactionEntryTestFixture()

        def inventoryService = new InventoryService()

        def map = inventoryService.getQuantityByProductMap(TransactionEntry.list())

        assert map[aspirinProduct] == 97
        assert map[tylenolProduct] == 25
    }

    //todo: getQuantity is broken now, need to know why
    void xtest_getQuantityByInventoryItem(){
        transactionEntryTestFixture()
        def inventoryService = new InventoryService()
        assert inventoryService.getQuantity(bostonInventory, aspirinItem1) == 94
        assert inventoryService.getQuantity(bostonInventory, aspirinItem2) == 3
        assert inventoryService.getQuantity(bostonInventory, tylenolItem) == 25
    }

    void test_getProductsQuantityForInventory(){
        transactionEntryTestFixture()
        def inventoryService = new InventoryService()
        def results = inventoryService.getProductsQuantityForInventory(bostonInventory)
        assert results[aspirinProduct] == 97
        assert results[tylenolProduct] == 25
    }


    void test_getQuantityByInventoryItemMap() {

        transactionEntryTestFixture()

        def inventoryService = new InventoryService()

        // fetch the map
        def map = inventoryService.getQuantityByInventoryItemMap(TransactionEntry.list())

        assert map[aspirinItem1] == 94
        assert map[aspirinItem2] == 3
        assert map[tylenolItem] == 25

    }

    void test_getInventoryItemsWithQuantity() {

        transactionEntryTestFixture()

        def products = [aspirinProduct, tylenolProduct]

        def inventoryService = new InventoryService()
        def inventoryItems = inventoryService.getInventoryItemsWithQuantity(products, bostonInventory)
        assert inventoryItems.size() == 2
        assert inventoryItems[aspirinProduct]
        assert inventoryItems[tylenolProduct]
        assert inventoryItems[aspirinProduct].size() == 2
        assert inventoryItems[tylenolProduct].size() == 1
        assert inventoryItems[aspirinProduct].find{ it.id == aspirinItem1.id }.quantity == 94
        assert inventoryItems[aspirinProduct].find{ it.id == aspirinItem2.id }.quantity == 3
        assert inventoryItems[tylenolProduct].find{ it.id == tylenolItem.id }.quantity == 25


    }

    void test_isValidForLocalTransfer_shouldCheckIfTransactionSupportsLocalTransfer() {
        localTransferTestFixture()

        def inventoryService = new InventoryService()

        // a transaction that isn't of transfer in or transfer out type shouldn't be marked as valid
        assert inventoryService.isValidForLocalTransfer(transaction1) == false

        // a transaction that's source or destination isn't a warehouse shouldn't pass validation //todo: need revist later; by Peter
//        assert inventoryService.isValidForLocalTransfer(transaction2) == false
//        assert inventoryService.isValidForLocalTransfer(transaction3) == false

        // transfer in/transfer out transactions associated with warehouses should pass validation
        assert inventoryService.isValidForLocalTransfer(transaction4) == true
        assert inventoryService.isValidForLocalTransfer(transaction5) == true
    }

    	void test_saveLocalTransfer_shouldCreateNewLocalTransfer() {
            localTransferTestFixture()

    		def inventoryService = new InventoryService()

    		def warehouse = bostonLocation

    		assert warehouse.inventory != null
    		assert transaction4.inventory != null

    		// save a local transaction based on a Transfer In Transaction
    		inventoryService.saveLocalTransfer(transaction4)

    		// confirm that this transaction is now associated with a local transfer
    		assert inventoryService.isLocalTransfer(transaction4) == true
    		def localTransfer = inventoryService.getLocalTransfer(transaction4)

    		// confirm that the local transfer has the appropriate source and destination transaction
    		assert localTransfer.destinationTransaction == transaction4
    		def newTransaction = localTransfer.sourceTransaction
    		assert newTransaction.transactionType ==  transactionType_transferOut
    		assert newTransaction.inventory == haitiInventory
    		assert newTransaction.source == null
    		assert newTransaction.destination == bostonLocation

    		// now try a local transaction based on a Transfer Out Transaction
    		inventoryService.saveLocalTransfer(transaction5)

    		// confirm that this transaction is now associated with a local transfer
    		assert inventoryService.isLocalTransfer(transaction5) == true
    		localTransfer = inventoryService.getLocalTransfer(transaction5)

    		// confirm that the local transfer has the appropriate source and destination transaction
    		assert localTransfer.sourceTransaction == transaction5
    		newTransaction = localTransfer.destinationTransaction
    		assert newTransaction.transactionType == transactionType_transferIn
    		assert newTransaction.inventory == haitiInventory
    		assert newTransaction.source == bostonLocation
    		assert newTransaction.destination == null

    	}

    	void test_saveLocalTransfer_shouldEditExistingLocalTransfer() {
            localTransferTestFixture()

    		def inventoryService = new InventoryService()

    		def baseTransaction = transaction4

    		// first create a local transfer
    		inventoryService.saveLocalTransfer(baseTransaction)

    		// now modify the base transaction
    		baseTransaction.inventory = haitiInventory
    		baseTransaction.source = bostonLocation

    		// resave the local transfer
    		inventoryService.saveLocalTransfer(baseTransaction)

    		// now check that the local transfer transactions have been updated accordingly
    		def localTransfer = inventoryService.getLocalTransfer(baseTransaction)
    		assert localTransfer.destinationTransaction == baseTransaction
    		def newTransaction = localTransfer.sourceTransaction
    		assert newTransaction.transactionType == transactionType_transferOut
    		assert newTransaction.inventory == bostonInventory
    		assert newTransaction.source == null
    		assert newTransaction.destination == haitiLocation
    	}
		
		
		
		void test_getProductsByTags() { 
			productTagTestFixture()
			def inventoryService = new InventoryService()
			def tags = new ArrayList()
			tags.add("thistag") 
			tags.add("thattag")
			
			def results = inventoryService.getProductsByTags(tags)
			assertEquals 2, results.size()
		}

		
		void test_getProductsByTag() { 
			productTagTestFixture()
			def tags = Tag.list()
			assertEquals 2, tags.size()
			
			def inventoryService = new InventoryService()
			def results = inventoryService.getProductsByTag("thistag")
			assertEquals 1, results.size()
		}
}
