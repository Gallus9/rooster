// Parse Cloud Code for transfer verification

Parse.Cloud.define("verifyTransfer", async (request) => {
  const orderId = request.params.orderId;
  const color = request.params.color;
  const condition = request.params.condition;
  const user = request.user;
  const Order = Parse.Object.extend("Order");
  const query = new Parse.Query(Order);
  const order = await query.get(orderId, { useMasterKey: true });
  if (!order) throw new Parse.Error(404, "Order not found");
  // Only buyer or seller can verify
  if (
    order.get("buyer").id !== user.id &&
    order.get("seller").id !== user.id
  ) {
    throw new Parse.Error(403, "Unauthorized");
  }
  // Save verification details (optional)
  order.set("color", color);
  order.set("condition", condition);
  order.set("status", "verified");
  await order.save(null, { useMasterKey: true });
  return "Transfer verified";
});

